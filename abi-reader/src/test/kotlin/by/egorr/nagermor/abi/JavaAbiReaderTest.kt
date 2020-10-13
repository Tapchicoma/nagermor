package by.egorr.nagermor.abi

import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    internal fun `Should return interface name`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("BaseInterface.class"))

        assertEquals(
            "BaseInterface",
            classAbi.className
        )
    }

    @Test
    internal fun `Should put implemented interface into public types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("ImplementsBaseInterface.class"))

        assertTrue(
            classAbi.publicTypes.contains("BaseInterface")
        )
    }

    @Test
    internal fun `Should put private field type into private types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("WithPrivateField.class"))

        assertTrue(
            classAbi.privateTypes.contains("Base")
        )
    }

    @Test
    internal fun `Should put public field type into public types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("WithPublicField.class"))

        assertTrue { classAbi.publicTypes.contains("Base") }
    }

    @Test
    internal fun `Should put private constant type into private types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("WithPrivateConstant.class"))

        assertTrue { classAbi.privateTypes.contains("Base") }
    }

    @Test
    internal fun `Should put public constant type into public types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("WithPublicConstant.class"))

        assertTrue { classAbi.publicTypes.contains("ExtendsBase") }
    }

    private fun getTestJavaClass(
        name: String
    ): Path = Paths.get(this::class.java.classLoader.getResource("java-source-output/$name")!!.toURI())
}
