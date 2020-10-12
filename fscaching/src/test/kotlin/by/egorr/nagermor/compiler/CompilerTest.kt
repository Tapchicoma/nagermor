package by.egorr.nagermor.compiler

import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class CompilerTest {
    private val fs = Jimfs.newFileSystem()
    private val testSourcesDir = fs.getPath("/sources")

    private val testCompilerBackend = TestCompilerBackend()
    private val compiler = Compiler(testCompilerBackend)

    @Test
    internal fun `Should recompile all sources on classpath change`() {
        val testClassPath = generateTestClasspath()
        val testSourceFiles = generateTestSources()

        compiler.compileSources(
            testClassPath,
            true,
            testSourcesDir,
            testSourceFiles.associateWith { Compiler.SourceFileState.NOT_CHANGED }
        )

        assertEquals(
            testSourceFiles,
            testCompilerBackend.lastCompilationSourceFiles
        )
    }

    @Test
    internal fun `Should return 0 exit status without recompilation when classpath and source files was not changed`() {
        val testClassPath = generateTestClasspath()
        val testSourceFiles = generateTestSources()

        val compilationResult = compiler.compileSources(
            testClassPath,
            false,
            testSourcesDir,
            testSourceFiles.associateWith { Compiler.SourceFileState.NOT_CHANGED }
        )

        assertEquals(0, compilationResult)
        assertNull(testCompilerBackend.lastCompilationClassPath)
        assertNull(testCompilerBackend.lastCompilationOutputDir)
        assertNull(testCompilerBackend.lastCompilationSourceFiles)
    }

    private fun generateTestClasspath(): List<Path> {
        val classpathDir = fs.getPath("/classpath")

        return listOf(
            classpathDir.writeFile(
                "fake jar",
                "fake-jar-1.jar"
            ),
            classpathDir.writeFile(
                "fake jar",
                "fake-jar-2.jar"
            )
        )
    }

    private fun generateTestSources(): List<Path> = listOf(
        testSourcesDir.writeFile(
            "source file 1",
            "source1.java"
        ),
        testSourcesDir.writeFile(
            "source file 2",
            "source2.java"
        )
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

    private class TestCompilerBackend : CompilerBackend {
        var lastCompilationOutputDir: Path? = null
        var lastCompilationClassPath: List<Path>? = null
        var lastCompilationSourceFiles: List<Path>? = null

        override fun compile(
            outputDir: Path,
            classPath: List<Path>,
            sourceFiles: List<Path>
        ): Int {
            lastCompilationOutputDir = outputDir
            lastCompilationClassPath = classPath
            lastCompilationSourceFiles = sourceFiles

            return 0
        }
    }
}
