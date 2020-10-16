package by.egorr.nagermor.compiler.incremental

import by.egorr.nagermor.abi.AbiReader
import com.google.common.jimfs.Jimfs
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.nio.file.Files
import java.util.stream.Stream
import kotlin.test.assertEquals

internal class AbiDependencyGraphTest {
    private val graph = AbiDependencyGraph(hashMapOf())

    @BeforeEach
    internal fun setUp() {
        fillGraphWithTestData()
    }

    @ParameterizedTest(name = "Should correctly return classes to recompile on class update")
    @MethodSource("provideUpdateTestData")
    internal fun recompileClassesOnUpdate(
        classToDelete: String,
        expectedClassesToRecompile: Set<String>
    ) {
        val classesToRecompile = graph.getClassesToRecompileOnClassChange(classToDelete)

        assertEquals(
            expectedClassesToRecompile,
            classesToRecompile
        )
    }

    @DisplayName("Should correctly serialize and deserialize graph")
    @Test
    internal fun serialization() {
        val fs = Jimfs.newFileSystem()
        val testCacheDir = fs.getPath("/cache")
        Files.createDirectories(testCacheDir)
        val testCacheFile = testCacheDir.resolve("test-cache.bin")

        graph.serialize(testCacheFile)

        val newGraph = AbiDependencyGraph.deserialize(testCacheFile)

        assertEquals(
            graph,
            newGraph
        )
    }

    @DisplayName("Should return no classes to recompile for already deleted node")
    @Test
    internal fun deleteNode() {
        graph.deleteNode("com/example/OneCyclic")

        val classesToRecompile = graph.getClassesToRecompileOnClassChange("com/example/OneCyclic")

        assertEquals(
            emptySet(),
            classesToRecompile
        )
    }

    private fun fillGraphWithTestData() {
        graph.updateOrAddNode(AbiReader.SourceFileAbi(
            className = "com/example/Base",
            privateTypes = emptySet(),
            publicTypes = setOf("java/lang/Object")
        ))
        graph.updateOrAddNode(AbiReader.SourceFileAbi(
            className = "com/example/ExtendsBase",
            privateTypes = emptySet(),
            publicTypes = setOf(
                "java/lang/Object",
                "com/example/Base",
                "com/example/OneCyclic"
            )
        ))
        graph.updateOrAddNode(AbiReader.SourceFileAbi(
            className = "com/example/BaseInterface",
            privateTypes = emptySet(),
            publicTypes = emptySet()
        ))
        graph.updateOrAddNode(AbiReader.SourceFileAbi(
            className = "com/example/OneCyclic",
            privateTypes = emptySet(),
            publicTypes = setOf("com/example/TwoCyclic")
        ))
        graph.updateOrAddNode(AbiReader.SourceFileAbi(
            className = "com/example/TwoCyclic",
            privateTypes = emptySet(),
            publicTypes = setOf("com/example/OneCyclic")
        ))
        graph.updateOrAddNode(AbiReader.SourceFileAbi(
            className = "com/example/ImplementsBaseInterface",
            privateTypes = setOf("com/example/ExtendsBase"),
            publicTypes = setOf("com/example/BaseInterface")
        ))
    }

    companion object {
        @JvmStatic
        fun provideUpdateTestData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "com/example/Base",
                setOf("com/example/ExtendsBase", "com/example/ImplementsBaseInterface")
            ),
            Arguments.of(
                "com/example/ExtendsBase",
                setOf("com/example/ImplementsBaseInterface")
            ),
            Arguments.of(
                "com/example/OneCyclic",
                setOf("com/example/TwoCyclic", "com/example/ExtendsBase", "com/example/ImplementsBaseInterface")
            ),
            Arguments.of(
                "com/example/ImplementsBaseInterface",
                emptySet<String>()
            ),
            Arguments.of(
                "com/example/TwoCyclic",
                setOf(
                    "com/example/OneCyclic",
                    "com/example/ExtendsBase",
                    "com/example/ImplementsBaseInterface"
                )
            )
        )
    }
}
