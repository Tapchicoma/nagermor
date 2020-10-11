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

    @Test
    internal fun `Should return all source files as added on first run`() {
        val allFiles = sourcesDir.initialSources()
        val sources = fileSystemChangesDetector.getSourceStatus(sourcesDir)
        val expected = allFiles
            .filter { it.toString().endsWith(".java") }
            .associateWith { FileSystemChangesDetector.SourceFileState.ADDED }

        assertEquals(
            expected,
            sources
        )
    }

    @Test
    internal fun `Should return all source files are not changed on the second run over same sources`() {
        val allFiles = sourcesDir.initialSources()
        fileSystemChangesDetector.getSourceStatus(sourcesDir)
        val sources = fileSystemChangesDetector.getSourceStatus(sourcesDir)

        val expected = allFiles
            .filter { it.toString().endsWith(".java") }
            .associateWith { FileSystemChangesDetector.SourceFileState.NOT_CHANGED }

        assertEquals(
            expected,
            sources
        )
    }

    @Test
    internal fun `Should indicate some file is change if content of this source file was changed`() {
        val allFiles = sourcesDir.initialSources()
        fileSystemChangesDetector.getSourceStatus(sourcesDir)
        Files.write(allFiles[1], "class Changed1();".toByteArray())
        val sources = fileSystemChangesDetector.getSourceStatus(sourcesDir)

        val expected = allFiles
            .filter { it.toString().endsWith(".java") }
            .associateWith {
                if (it != allFiles[1]) {
                    FileSystemChangesDetector.SourceFileState.NOT_CHANGED
                } else {
                    FileSystemChangesDetector.SourceFileState.CHANGED
                }
            }

        assertEquals(
            expected,
            sources
        )
    }

    @Test
    internal fun `Should differentiate caching between different source dirs`() {
        sourcesDir.initialSources()
        fileSystemChangesDetector.getSourceStatus(sourcesDir)
        val sourceDir2 = fs.getPath("/sources2")
        val allFiles = sourceDir2.initialSources()
        val sources = fileSystemChangesDetector.getSourceStatus(sourceDir2)

        val expected = allFiles
            .filter { it.toString().endsWith(".java") }
            .associateWith {
                FileSystemChangesDetector.SourceFileState.ADDED
            }

        assertEquals(
            expected,
            sources
        )
    }

    @Test
    internal fun `Should mark source file as deleted`() {
        val allFiles = sourcesDir.initialSources()
        fileSystemChangesDetector.getSourceStatus(sourcesDir)

        Files.delete(allFiles.last())
        val sources = fileSystemChangesDetector.getSourceStatus(sourcesDir)

        val expected = allFiles
            .filter { it.toString().endsWith(".java") }
            .associateWith {
                if (it != allFiles.last()) {
                    FileSystemChangesDetector.SourceFileState.NOT_CHANGED
                } else {
                    FileSystemChangesDetector.SourceFileState.REMOVED
                }
            }

        assertEquals(
            expected,
            sources
        )
    }

    private fun testClassPath(
        classpathDir: String = "/classpath1"
    ): List<Path> {
        val classpathPath = fs.getPath(classpathDir).apply { Files.createDirectories(this) }

        return listOf(
            classpathPath.writeFile(
                "some test content 1 $classpathDir",
                "some-file-1.txt"
            ),
            classpathPath.writeFile(
                "some test content 2 $classpathDir",
                "some-file-2.txt"
            ),
        )
    }

    private fun Path.initialSources(): List<Path> = listOf(
        writeFile(
            "class Test1();",
            "Test1.java",
            "src", "main", "java", "1"
        ),
        writeFile(
            "class Test2();",
            "Test2.java",
            "src", "main", "java", "2"
        ),
        writeFile(
            "Readme",
            "README.md",
            "docs"
        ),
    )

    private fun Path.writeFile(
        content: String,
        fileName: String,
        vararg parentDirs: String
    ) = parentDirs
        .fold(this) { acc, dir -> acc.resolve(dir) }
        .apply {
            Files.createDirectories(this)
        }
        .resolve(fileName)
        .apply {
            Files.createFile(this)
            Files.write(this, content.toByteArray())
        }
}
