package by.egorr.nagermor.compiler.incremental

import by.egorr.nagermor.abi.AbiReader
import by.egorr.nagermor.abi.JavaAbiReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.lang.IllegalStateException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.streams.asSequence

@OptIn(ExperimentalSerializationApi::class)
class IncrementalCompilationSolver(
    private val incrementalCacheFile: Path,
    private val outputFileExtension: String = "class",
    private val abiReader: AbiReader = JavaAbiReader(),
) {
    private val classToSourceFileMap: MutableMap<String, Path>
    private val graph: AbiDependencyGraph

    init {
        if (Files.exists(incrementalCacheFile)) {
            val savedData = Cbor.decodeFromByteArray<IncrementalCacheData>(Files.readAllBytes(incrementalCacheFile))
            classToSourceFileMap = savedData.classToSourceFileMap.mapValues { Paths.get(it.value) }.toMutableMap()
            graph = AbiDependencyGraph.deserialize(savedData.graphSerializedData)
        } else {
            classToSourceFileMap = hashMapOf()
            graph = AbiDependencyGraph(hashMapOf())
        }
    }

    fun sourceFileChanged(
        sourceFile: Path
    ): Set<Path> {
        val className = if (Files.exists(sourceFile)) {
            sourceFile.getJavaSourceFileTopElementName()
        } else {
            // Source file was deleted, looking for it in the cache map
            getDeletedSourceFileClassNameFromCache(sourceFile)
        }
        return graph
            .getClassesToRecompileOnClassChange(className)
            .map {
                classToSourceFileMap[it] ?: throw IllegalStateException(
                    "Class $it source file is unknown!"
                )
            }
            .toSet()
    }

    fun updateSourceFilesAbi(
        outputDir: Path,
        sourceFiles: Set<Path>
    ) {
        sourceFiles
            .associateWith { it.getJavaSourceFileTopElementName() }
            .mapValues {
                deletePreviousOutputsIfPackageHasChanged(outputDir, it.key, it.value)
                classToSourceFileMap[it.value] = it.key
                abiReader.parseSourceFileAbi(outputDir.resolve("${it.value}.$outputFileExtension"))
            }
            .forEach {
                graph.updateOrAddNode(it.value)
            }
    }

    fun removeSourceFiles(
        outputDir: Path,
        sourceFiles: Set<Path>
    ) {
        sourceFiles
            .map { getDeletedSourceFileClassNameFromCache(it) }
            .forEach {
                deleteWithInnerClasses(outputDir, it)
                classToSourceFileMap.remove(it)
                graph.deleteNode(it)
            }
    }

    fun saveState() {
        if (Files.notExists(incrementalCacheFile)) {
            Files.createFile(incrementalCacheFile)
        }

        val dataToSave = IncrementalCacheData(
            classToSourceFileMap.mapValues { it.value.toString() },
            graph.serialize()
        )

        Files.write(
            incrementalCacheFile,
            Cbor.encodeToByteArray(dataToSave),
            StandardOpenOption.TRUNCATE_EXISTING
        )
    }

    /**
     * Resets incremental cache state.
     */
    fun resetInternalState() {
        graph.clear()
        classToSourceFileMap.clear()
        Files.deleteIfExists(incrementalCacheFile)
    }

    private fun getDeletedSourceFileClassNameFromCache(sourceFile: Path): String {
        val deletedClassMap = classToSourceFileMap.filterValues { it == sourceFile }
        check(deletedClassMap.size == 1) {
            "Removed $sourceFile is not known for this compiler"
        }
        return deletedClassMap.keys.first()
    }

    private fun deletePreviousOutputsIfPackageHasChanged(
        outputDir: Path,
        sourceFile: Path,
        newClassName: String
    ) {
        val existingMapping = classToSourceFileMap.filterValues { it == sourceFile }
        if (existingMapping.size == 1) {
            val oldClassName = existingMapping.keys.first()
             if (oldClassName != newClassName) {
                deleteWithInnerClasses(
                    outputDir,
                    oldClassName
                )
                classToSourceFileMap.remove(oldClassName)
            }
        }
    }

    private fun deleteWithInnerClasses(
        outputDir: Path,
        parentClass: String
    ) {
        val parentClassOutputFile = outputDir.resolve("$parentClass.$outputFileExtension")
        val className = parentClass.substringAfterLast('/')

        Files
            .list(parentClassOutputFile.parent)
            .asSequence()
            .filter { Files.isRegularFile(it) }
            .filter {
                it.fileName.toString().startsWith(className)
            }
            .forEach {
                Files.delete(it)
            }
    }

    @Serializable
    private class IncrementalCacheData(
        val classToSourceFileMap: Map<String, String>,
        val graphSerializedData: ByteArray
    )
}
