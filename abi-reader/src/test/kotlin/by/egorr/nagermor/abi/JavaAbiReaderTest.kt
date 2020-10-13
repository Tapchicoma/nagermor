package by.egorr.nagermor.abi

import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.asserter

internal class JavaAbiReaderTest {
    private val abiReader = JavaAbiReader()

    @Test
    internal fun `Should return class name`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("Base.class"))

        assertEquals(
            "Base",
            classAbi.className
        )
    }

    @Test
    internal fun `Should return class source file name`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("Base.class"))

        assertEquals(
            "Base.java",
            classAbi.sourceFileName
        )
    }

    @Test
    internal fun `Should put extended class into public types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("ExtendsBase.class"))

        assertTrue(
            classAbi.publicTypes.contains("Base")
        )
    }

    private fun getTestJavaClass(
        name: String
    ): Path = Paths.get(this::class.java.classLoader.getResource("java-compiled/$name")!!.toURI())
}
