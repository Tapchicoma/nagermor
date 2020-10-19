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

    @Test
    internal fun `Should parse public method type into public types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("ImplementsBaseInterface.class"))

        assertTrue { classAbi.publicTypes.contains("Base") }
    }

    @Test
    internal fun `Should parse private method type into private types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("ImplementsBaseInterface.class"))

        assertTrue { classAbi.privateTypes.contains("ExtendsBase") }
    }

    @Test
    internal fun `Should put private method exceptions into private types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("ImplementsBaseInterface.class"))

        assertTrue { classAbi.privateTypes.contains("TestException") }
    }

    @Test
    internal fun `Should parse public method parameters into public types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("WithMethodParameters.class"))

        assertTrue { classAbi.publicTypes.containsAll(listOf("Base", "ImplementsBaseInterface")) }
    }

    @Test
    internal fun `Should parse method internal types`() {
        val classAbi = abiReader.parseSourceFileAbi(getTestJavaClass("TestClassWithMethodTypes.class"))

        assertEquals(
            setOf("ExtendsBase", "Base", "WithPrivateField"),
            classAbi.privateTypes
        )
    }

    @Test
    internal fun `Should parse correctly annotation`() {
        val classAbi = abiReader.parseSourceFileAbi(
            getTestJavaClass("by/egorr/test/annotations/TestAnnotation.class")
        )

        assertEquals(
            "by/egorr/test/annotations/TestAnnotation",
            classAbi.className
        )
        assertEquals(
            setOf(
                "java/lang/Object",
                "java/lang/annotation/Annotation",
                "java/lang/annotation/Retention",
                "java/lang/annotation/RetentionPolicy",
                "java/lang/String",
            ),
            classAbi.publicTypes
        )
        assertTrue(classAbi.privateTypes.isEmpty())
    }

    @Test
    internal fun `Should parse method annotations`() {
        val classAbi = abiReader.parseSourceFileAbi(
            getTestJavaClass("by/egorr/test/annotations/MethodWithAnnotation.class")
        )

        assertTrue {
            classAbi.publicTypes.contains(
                "by/egorr/test/annotations/TestAnnotation"
            )
        }
    }

    @Test
    internal fun `Should parse field annotations`() {
        val classAbi = abiReader.parseSourceFileAbi(
            getTestJavaClass("by/egorr/test/annotations/FieldWithAnnotation.class")
        )

        assertTrue {
            classAbi.publicTypes.contains(
                "by/egorr/test/annotations/TestAnnotation"
            )
        }
    }

    @Test
    internal fun `Should parse type annotation`() {
        val classAbi = abiReader.parseSourceFileAbi(
            getTestJavaClass("by/egorr/test/annotations/ConstructorWithTypeAnnotation.class")
        )

        assertTrue {
            classAbi.publicTypes.contains(
                "by/egorr/test/annotations/TestTypeAnnotation"
            )
        }
    }

    @Test
    internal fun `Should parse static inner class`() {
        val classAbi = abiReader.parseSourceFileAbi(
            getTestJavaClass("StaticInnerClass.class")
        )

        assertEquals(
            setOf("StaticInnerClass\$InnerTest"),
            classAbi.innerClassNames
        )
    }

    @Test
    internal fun `Should parse anonymous inner class`() {
        val classAbi = abiReader.parseSourceFileAbi(
            getTestJavaClass("ClassWithAnonymousClass.class")
        )

        assertEquals(
            setOf("ClassWithAnonymousClass$1"),
            classAbi.innerClassNames
        )
    }

    private fun getTestJavaClass(
        name: String
    ): Path = Paths.get(this::class.java.classLoader.getResource("java-source-output/$name")!!.toURI())
}
