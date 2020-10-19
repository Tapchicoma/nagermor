package by.egorr.nagermor.fscaching

import by.egorr.nagermor.compiler.Compiler
import by.egorr.nagermor.fscaching.hash.HashHelper
import by.egorr.nagermor.fscaching.hash.HashHelper.Companion.toHex
import by.egorr.nagermor.fscaching.hash.Sha256HashHelper
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Detects changes since previous compilation in provided source files and classpath.
 *
 * @param rootCachePath dir under which class should store caches between compilations
 */
class FileSystemChangesDetector(
    private val rootCachePath: Path,
    private val hashHelper: HashHelper = Sha256HashHelper()
) {
    private var computedClassPathHash: ByteArray? = null
    private var computedSourcesHashes: Map<Path, ByteArray>? = null

    /**
     * Detect if [classpath] is the same as from previous compilation for given [sourcesPath].
     */
    fun isClassPathChanged(
        sourcesPath: Path,
        outputPath: Path,
        classpath: List<Path>
    ): Boolean {
        val classpathHashFile = classPathHashFile(sourcesPath, outputPath)

        computedClassPathHash = hashHelper.hashString(
            classpath.joinToString {
                hashHelper.hashFile(it).toHex()
            }
        )

        val previousCompilationClassPathHash = if (Files.exists(classpathHashFile)) {
            Files.readAllBytes(classpathHashFile)
        } else {
            ByteArray(0)
        }

        return previousCompilationClassPathHash.isEmpty() ||
            !computedClassPathHash.contentEquals(previousCompilationClassPathHash)
    }

    /**
     * Get a map of all files that [sourcesPath] contains with respective file state related to previous
     * compilation for given [sourcesPath].
     */
    fun getSourceStatus(
        sourcesPath: Path,
        outputPath: Path,
        fileExtension: String = "java"
    ): Map<Path, Compiler.SourceFileState> {
        val fileVisitor = SourcesFileVisitor(fileExtension)
        Files.walkFileTree(sourcesPath, fileVisitor)

        val cacheFile = sourcesHashFile(sourcesPath, outputPath)

        val previousCompilationSourceFilesHashes = cacheFile.readSourceFilesCache().toMutableMap()
        val sourceFilesHashes = fileVisitor.sourceFiles.associateWith { hashHelper.hashFile(it) }
        computedSourcesHashes = sourceFilesHashes

        val result = sourceFilesHashes
            .mapValues { entry ->
                when {
                    previousCompilationSourceFilesHashes.containsKey(entry.key) -> if (
                        entry.value.contentEquals(previousCompilationSourceFilesHashes[entry.key])
                    ) {
                        Compiler.SourceFileState.NOT_CHANGED
                    } else {
                        Compiler.SourceFileState.CHANGED
                    }.also {
                        previousCompilationSourceFilesHashes.remove(entry.key)
                    }
                    else -> Compiler.SourceFileState.ADDED
                }
            }
            .toMutableMap()

        if (previousCompilationSourceFilesHashes.isNotEmpty()) {
            result.putAll(
                previousCompilationSourceFilesHashes
                    .mapValues { Compiler.SourceFileState.REMOVED }
            )
        }

        return result.toMap()
    }

    /**
     * Clears previous compilation caches for given [sourcePath].
     */
    fun clearCache(
        sourcePath: Path,
        outputPath: Path
    ) {
        Files.deleteIfExists(classPathHashFile(sourcePath, outputPath))
        Files.deleteIfExists(sourcesHashFile(sourcePath, outputPath))
    }

    /**
     * Save current calculated state into cache files.
     *
     * Should be called either after [isClassPathChanged], or [getSourceStatus], or after both.
     */
    fun saveState(
        sourcesPath: Path,
        outputPath: Path
    ) {
        computedClassPathHash?.run {
            val cacheFile = classPathHashFile(sourcesPath, outputPath)
            if (Files.notExists(cacheFile)) Files.createFile(cacheFile)
            Files.write(cacheFile, this)
        }
        computedSourcesHashes?.run {
            sourcesHashFile(sourcesPath, outputPath).writeSourceFilesCache(this)
        }
    }

    private fun classPathHashFile(
        sourcesPath: Path,
        outputPath: Path
    ): Path = sourcesCacheDir(sourcesPath, outputPath)
        .resolve("classpath_hash.bin")
        .apply {
            if (!Files.exists(this)) {
                Files.createFile(this)
            }
        }

    private fun sourcesHashFile(
        sourcesPath: Path,
        outputPath: Path
    ): Path = sourcesCacheDir(sourcesPath, outputPath)
        .resolve("sources_hash.bin")

    private fun sourcesCacheDir(
        sourcesPath: Path,
        outputPath: Path
    ) = rootCachePath
        .resolve(
            hashHelper.hashString("${sourcesPath.toAbsolutePath()}${outputPath.toAbsolutePath()}").toHex()
        )
        .apply {
            if (!Files.exists(this)) {
                Files.createDirectories(this)
            }
        }

    @OptIn(ExperimentalSerializationApi::class)
    private fun Path.readSourceFilesCache(): Map<Path, ByteArray> {
        if (!Files.exists(this)) return emptyMap()

        val savedMap: Map<String, ByteArray> = Cbor.decodeFromByteArray(Files.readAllBytes(this))
        return savedMap.mapKeys { fileSystem.getPath(it.key) }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun Path.writeSourceFilesCache(
        sourceFilesHashes: Map<Path, ByteArray>
    ) {
        if (!Files.exists(this)) Files.createFile(this)

        Files.write(
            this,
            Cbor.encodeToByteArray(
                sourceFilesHashes.mapKeys { it.key.toString() }
            )
        )
    }

    private class SourcesFileVisitor(
        fileExtension: String
    ) : SimpleFileVisitor<Path>() {
        private val fileExtensionWithDot = ".$fileExtension"
        private val _sourceFiles = mutableListOf<Path>()
        val sourceFiles get() = _sourceFiles.toList()

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            return if (attrs.isRegularFile && !file.toString().endsWith(fileExtensionWithDot)) {
                FileVisitResult.SKIP_SIBLINGS
            } else {
                _sourceFiles.add(file)
                FileVisitResult.CONTINUE
            }
        }
    }
}
