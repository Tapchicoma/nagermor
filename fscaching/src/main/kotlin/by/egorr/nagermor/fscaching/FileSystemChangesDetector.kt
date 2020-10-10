package by.egorr.nagermor.fscaching

import java.nio.file.Path

/**
 * Detects changes since previous compilation in provided source files and classpath.
 *
 * @param rootCachePath dir under which class should store caches between compilations
 */
class FileSystemChangesDetector(
    rootCachePath: Path
) {
    /**
     * Detect if [classpath] is the same as from previous compilation for given [sourcesPath].
     */
    fun isClassPathChanged(
        sourcesPath: Path,
        classpath: List<String>
    ): Boolean = true

    /**
     * Get a map of all files that [sourcesPath] contains with respective file state related to previous
     * compilation for given [sourcesPath].
     */
    fun getSourceStatus(
        sourcesPath: Path,
        fileExtension: String = ".java"
    ): Map<Path, SourceFileState> = emptyMap()

    enum class SourceFileState {
        ADDED, REMOVED, CHANGED, NOT_CHANGED;
    }
}
