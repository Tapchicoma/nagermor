package by.egorr.nagermor.fscaching

import by.egorr.nagermor.fscaching.hash.HashHelper
import by.egorr.nagermor.fscaching.hash.HashHelper.Companion.toHex
import by.egorr.nagermor.fscaching.hash.Sha256HashHelper
import java.nio.file.Files
import java.nio.file.Path

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
    ): Map<Path, SourceFileState> = emptyMap()

    private fun Path.classPathHashFile(): Path = sourcesCacheDir().resolve("classpath_hash.bin").apply {
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

    enum class SourceFileState {
        ADDED, REMOVED, CHANGED, NOT_CHANGED;
    }
}
