package by.egorr.nagermor.compiler.incremental

import by.egorr.nagermor.abi.AbiReader
import by.egorr.nagermor.compiler.incremental.AbiDependencyGraph
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

internal class AbiDependencyGraphTest {
    private val graph = AbiDependencyGraph()

    @BeforeEach
    internal fun setUp() {
        fillGraphWithTestData()
    }

    @ParameterizedTest(name = "Should correctly return classes to recompile on node delete")
    @MethodSource("provideDeleteTestData")
    internal fun recompileClassesOnDelete(
        classToDelete: String,
        expectedClassesToRecompile: Set<String>
    ) {
        val classesToRecompile = graph.deleteNode(classToDelete)

        assertEquals(
            expectedClassesToRecompile,
            classesToRecompile
        )
    }

    @ParameterizedTest(name = "Should correctly return classes to recompile on node update")
    @MethodSource("provideUpdateTestData")
    internal fun recompileClassesOnUpdate(
        updateClassAbi: AbiReader.SourceFileAbi,
        expectedClassesToRecompile: Set<String>
    ) {
        val classesToRecompile = graph.updateNode(updateClassAbi)

        assertEquals(
            expectedClassesToRecompile,
            classesToRecompile
        )
    }

    private fun fillGraphWithTestData() {
        graph.addNode(AbiReader.SourceFileAbi(
            className = "com/example/Base",
            privateTypes = emptySet(),
            publicTypes = setOf("java/lang/Object")
        ))
        graph.addNode(AbiReader.SourceFileAbi(
            className = "com/example/ExtendsBase",
            privateTypes = emptySet(),
            publicTypes = setOf(
                "java/lang/Object",
                "com/example/Base",
                "com/example/OneCyclic"
            )
        ))
        graph.addNode(AbiReader.SourceFileAbi(
            className = "com/example/BaseInterface",
            privateTypes = emptySet(),
            publicTypes = emptySet()
        ))
        graph.addNode(AbiReader.SourceFileAbi(
            className = "com/example/OneCyclic",
            privateTypes = emptySet(),
            publicTypes = setOf("com/example/TwoCyclic")
        ))
        graph.addNode(AbiReader.SourceFileAbi(
            className = "com/example/TwoCyclic",
            privateTypes = emptySet(),
            publicTypes = setOf("com/example/OneCyclic")
        ))
        graph.addNode(AbiReader.SourceFileAbi(
            className = "com/example/ImplementsBaseInterface",
            privateTypes = setOf("com/example/ExtendsBase"),
            publicTypes = setOf("com/example/BaseInterface")
        ))
    }

    companion object {
        @JvmStatic
        fun provideDeleteTestData(): Stream<Arguments> = Stream.of(
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
            )
        )

        @JvmStatic
        fun provideUpdateTestData(): Stream<Arguments> = Stream.of(
            Arguments.of(
                AbiReader.SourceFileAbi(
                    "com/example/ImplementsBaseInterface",
                    privateTypes = emptySet(),
                    publicTypes = setOf("com/example/BaseInterface", "com/example/TwoCyclic")
                ),
                emptySet<String>()
            ),
            Arguments.of(
                AbiReader.SourceFileAbi(
                    "com/example/TwoCyclic",
                    privateTypes = setOf("com/example/Base"),
                    publicTypes = emptySet()
                ),
                setOf(
                    "com/example/OneCyclic",
                    "com/example/ExtendsBase",
                    "com/example/ImplementsBaseInterface"
                )
            )
        )
    }
}
