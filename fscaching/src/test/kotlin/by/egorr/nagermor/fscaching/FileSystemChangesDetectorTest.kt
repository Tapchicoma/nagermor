package by.egorr.nagermor.fscaching

import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

internal class FileSystemChangesDetectorTest {
    private val fs = Jimfs.newFileSystem()
    private val cacheDir = fs.getPath("/cache").apply { Files.createDirectories(this) }
    private val sourcesDir = fs.getPath("/sources").apply { Files.createDirectories(this) }

    private val fileSystemChangesDetector = FileSystemChangesDetector(cacheDir)

    @Test
    internal fun `Should always return true if no previous compilation cache is stored`() {
        val isChanged = fileSystemChangesDetector.isClassPathChanged(sourcesDir, emptyList())

        assertEquals(true, isChanged)
    }

    @Test
    internal fun `Should return false for next compilation with same classpath and sourcesDir`() {
        val classPath = testClassPath()
        fileSystemChangesDetector.isClassPathChanged(sourcesDir, classPath)

        val isChanged = fileSystemChangesDetector.isClassPathChanged(sourcesDir, classPath)

        assertEquals(
            false,
            isChanged
        )
    }

    @Test
    internal fun `Should return true when classpath is the same, but sourcesDir is different`() {
        val classPath = testClassPath()
        fileSystemChangesDetector.isClassPathChanged(sourcesDir, classPath)
        val anotherSourcesDir = fs.getPath("/sources2").apply { Files.createDirectories(this) }

        val isChanged = fileSystemChangesDetector.isClassPathChanged(anotherSourcesDir, classPath)

        assertEquals(
            true,
            isChanged
        )
    }

    @Test
    internal fun `Should return true when classpath changes, but sourcesDir is the same`() {
        fileSystemChangesDetector.isClassPathChanged(sourcesDir, testClassPath())

        val isChanged = fileSystemChangesDetector.isClassPathChanged(sourcesDir, testClassPath("/classpath2"))

        assertEquals(
            true,
            isChanged
        )
    }

    @Test
    internal fun `Should return true when content of one of classpath file changes`() {
        val testClassPath = testClassPath()
        fileSystemChangesDetector.isClassPathChanged(sourcesDir, testClassPath)
        Files.write(testClassPath[0], "new content".toByteArray())

        val isChanged = fileSystemChangesDetector.isClassPathChanged(sourcesDir, testClassPath)

        assertEquals(
            true,
            isChanged
        )
    }

    private fun testClassPath(
        classpathDir: String = "/classpath1"
    ): List<Path> {
        val classpathPath = fs.getPath(classpathDir).apply { Files.createDirectories(this) }

        return listOf(
            classpathPath.resolve("some-file-1.txt").apply {
                Files.createFile(this)
                Files.write(this, "some test content 1 $classpathDir".toByteArray())
            },
            classpathPath.resolve("some-file-2.txt").apply {
                Files.createFile(this)
                Files.write(this, "some test content 2 $classpathDir".toByteArray())
            }
        )
    }
}
