package by.egorr.nagermor.compiler.incremental

import by.egorr.nagermor.abi.AbiReader
import by.egorr.nagermor.abi.JavaAbiReader
import com.github.javaparser.StaticJavaParser
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
        val className = sourceFile.getSourceFileClassName()
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
            .associateWith { it.getSourceFileClassName() }
            .mapValues {
                classToSourceFileMap[it.value] = it.key
                abiReader.parseSourceFileAbi(outputDir.resolve("${it.value}.$outputFileExtension"))
            }
            .forEach {
                graph.updateOrAddNode(it.value)
            }
    }

    fun removeSourceFiles(
        sourceFiles: Set<Path>
    ) {
        sourceFiles
            .map { it.getSourceFileClassName() }
            .forEach {
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

    @Serializable
    private class IncrementalCacheData(
        val classToSourceFileMap: Map<String, String>,
        val graphSerializedData: ByteArray
    )

    private fun Path.getSourceFileClassName(): String {
        val parsedSource = StaticJavaParser.parse(this)

        val packageDeclaration = parsedSource.packageDeclaration
        val packageName = if (packageDeclaration.isPresent) {
            packageDeclaration.get().nameAsString
        } else {
            null
        }

        val classFileName = fileName.toString().substringBeforeLast('.')
        var className: String? = null

        val classDeclaration = parsedSource.getClassByName(classFileName)
        if (classDeclaration.isPresent) className = classDeclaration.get().nameAsString

        if (className == null) {
            val interfaceDeclaration = parsedSource.getInterfaceByName(classFileName)
            if (interfaceDeclaration.isPresent) className = interfaceDeclaration.get().nameAsString
        }

        if (className == null) {
            val enumDeclaration = parsedSource.getEnumByName(classFileName)
            if (enumDeclaration.isPresent) className = enumDeclaration.get().nameAsString
        }

        if (className == null) {
            val annotationDeclaration = parsedSource.getAnnotationDeclarationByName(classFileName)
            if (annotationDeclaration.isPresent) className = annotationDeclaration.get().nameAsString
        }

        requireNotNull(className) {
            "Could not find class name for $this source file."
        }

        return if (packageName.isNullOrBlank()) {
            className
        } else {
            "$packageName.$className"
        }.replace('.', '/')
    }
}
