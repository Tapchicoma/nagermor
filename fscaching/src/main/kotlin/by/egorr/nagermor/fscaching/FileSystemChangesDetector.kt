package by.egorr.nagermor.fscaching

import by.egorr.nagermor.fscaching.hash.HashHelper
import by.egorr.nagermor.fscaching.hash.HashHelper.Companion.toHex
import by.egorr.nagermor.fscaching.hash.Sha256HashHelper
import org.apache.commons.codec.binary.Base64
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
    private val rootCachePath: Path
) {
    private val hashHelper: HashHelper = Sha256HashHelper()

    /**
     * Detect if [classpath] is the same as from previous compilation for given [sourcesPath].
     */
    fun isClassPathChanged(
        sourcesPath: Path,
        classpath: List<Path>
    ): Boolean {
        val classpathHashFile = sourcesPath.classPathHashFile()

        val currentClassPathHash = hashHelper.hashString(
            classpath.joinToString {
                hashHelper.hashFile(it).toHex()
            }
        )

        val previousCompilationClassPathHash = Files.readAllBytes(classpathHashFile)
        return if (previousCompilationClassPathHash.isNotEmpty() &&
            currentClassPathHash.contentEquals(previousCompilationClassPathHash)) {
            false
        } else {
            Files.write(classpathHashFile, currentClassPathHash)
            true
        }
    }

    /**
     * Get a map of all files that [sourcesPath] contains with respective file state related to previous
     * compilation for given [sourcesPath].
     */
    fun getSourceStatus(
        sourcesPath: Path,
        fileExtension: String = ".java"
    ): Map<Path, SourceFileState> {
        val fileVisitor = SourcesFileVisitor(fileExtension)
        Files.walkFileTree(sourcesPath, fileVisitor)

        val cacheFile = sourcesPath.sourcesHashFile()

        val previousCompilationSourceFilesHashes = cacheFile.readSourceFilesCache().toMutableMap()
        val sourceFilesHashes = fileVisitor.sourceFiles.associateWith { hashHelper.hashFile(it) }

        val result = sourceFilesHashes
            .mapValues { entry ->
                when {
                    previousCompilationSourceFilesHashes.containsKey(entry.key) -> if (
                        entry.value.contentEquals(previousCompilationSourceFilesHashes[entry.key])
                    ) {
                        SourceFileState.NOT_CHANGED
                    } else {
                        SourceFileState.CHANGED
                    }.also {
                        previousCompilationSourceFilesHashes.remove(entry.key)
                    }
                    else -> SourceFileState.ADDED
                }
            }
            .toMutableMap()

        if (previousCompilationSourceFilesHashes.isNotEmpty()) {
            result.putAll(
                previousCompilationSourceFilesHashes
                    .mapValues { SourceFileState.REMOVED }
            )
        }

        if (result.values.filterNot { it == SourceFileState.NOT_CHANGED }.isNotEmpty()) {
            cacheFile.writeSourceFilesCache(sourceFilesHashes)
        }

        return result.toMap()
    }

    private fun Path.classPathHashFile(): Path = sourcesCacheDir().resolve("classpath_hash.bin").apply {
        if (!Files.exists(this)) {
            Files.createFile(this)
        }
    }

    private fun Path.sourcesHashFile(): Path = sourcesCacheDir().resolve("sources_hash.bin").apply {
        if (!Files.exists(this)) {
            Files.createFile(this)
        }
    }

    private fun Path.sourcesCacheDir() = rootCachePath.resolve(
        hashHelper.hashString(toAbsolutePath().toString()).toHex()
    ).apply {
        if (!Files.exists(this)) {
            Files.createDirectories(this)
        }
    }

    private fun Path.readSourceFilesCache(): Map<Path, ByteArray> {
        if (!Files.exists(this)) return emptyMap()

        Files.newBufferedReader(this).use { reader ->
            return reader
                .lineSequence()
                .associate {
                    val (path, hash) = it.split(";")
                    rootCachePath.fileSystem.getPath(path) to Base64.decodeBase64(hash)
                }
        }
    }

    private fun Path.writeSourceFilesCache(
        sourceFilesHashes: Map<Path, ByteArray>
    ) {
        if (!Files.exists(this)) Files.createFile(this)

        Files.newBufferedWriter(this).use { writer ->
            sourceFilesHashes.forEach {
                writer.write("${it.key};${Base64.encodeBase64String(it.value)}\n")
            }
        }
    }

    enum class SourceFileState {
        ADDED, REMOVED, CHANGED, NOT_CHANGED;
    }

    private class SourcesFileVisitor(
        private val fileExtension: String
    ) : SimpleFileVisitor<Path>() {
        private val _sourceFiles = mutableListOf<Path>()
        val sourceFiles get() = _sourceFiles.toList()

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            return if (attrs.isRegularFile && !file.toString().endsWith(fileExtension)) {
                FileVisitResult.SKIP_SIBLINGS
            } else {
                _sourceFiles.add(file)
                FileVisitResult.CONTINUE
            }
        }
    }
}
