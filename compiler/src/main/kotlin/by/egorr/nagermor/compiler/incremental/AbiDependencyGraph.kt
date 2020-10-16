package by.egorr.nagermor.compiler.incremental

import by.egorr.nagermor.abi.AbiReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray

internal class AbiDependencyGraph(
    private val graph: HashMap<ClassNode, MutableSet<AbiDependencyEdge>>
) {

    fun updateOrAddNode(classAbi: AbiReader.SourceFileAbi) {
        val nodeDependencies = graph[ClassNode(classAbi.className)]
        if (nodeDependencies == null ||
            nodeDependencies.filterIsInstance<AbiDependencyEdge.IncomingEdge>().isEmpty()
        ) {
            addNewNode(classAbi)
            return
        }

        val incomingDependencies = nodeDependencies.filterIsInstance<AbiDependencyEdge.IncomingEdge>()
        val incomingDependenciesTypes = incomingDependencies.map { it.dependsOnClass }

        val allTypes = classAbi.publicTypes + classAbi.privateTypes
        val removedDependencies = incomingDependenciesTypes.subtract(allTypes)
        val addedDependencies = allTypes.subtract(incomingDependenciesTypes)
        val possiblyChangedDependencies = allTypes.subtract(addedDependencies)

        removedDependencies.forEach { removedType ->
            nodeDependencies.removeIf { it is AbiDependencyEdge.IncomingEdge && it.dependsOnClass == removedType }
            graph[ClassNode(removedType)]?.run {
                removeIf { it is AbiDependencyEdge.OutgoingEdge && it.dependentClass == classAbi.className }
            }
        }

        addedDependencies.forEach { addedType ->
            val isPrivate = classAbi.privateTypes.contains(addedType)
            nodeDependencies.add(
                AbiDependencyEdge.IncomingEdge(
                    isPrivate = isPrivate,
                    dependsOnClass = addedType
                )
            )

            graph[ClassNode(addedType)]?.run {
                add(
                    AbiDependencyEdge.OutgoingEdge(
                        isPrivate = isPrivate,
                        dependentClass = classAbi.className
                    )
                )
            }
        }

        incomingDependencies
            .filter { edge ->
                possiblyChangedDependencies.contains(edge.dependsOnClass) &&
                    edge.isPrivate != classAbi.privateTypes.contains(edge.dependsOnClass)
            }
            .forEach { edge ->
                nodeDependencies.remove(edge)

                val isPrivate = classAbi.privateTypes.contains(edge.dependsOnClass)
                nodeDependencies.add(
                    AbiDependencyEdge.IncomingEdge(
                        isPrivate = isPrivate,
                        dependsOnClass = edge.dependsOnClass
                    )
                )

                graph[ClassNode(edge.dependsOnClass)]?.run {
                    removeIf { it is AbiDependencyEdge.OutgoingEdge && it.dependentClass == classAbi.className }
                    add(
                        AbiDependencyEdge.OutgoingEdge(
                            isPrivate = isPrivate,
                            dependentClass = classAbi.className
                        )
                    )
                }
            }

        graph[ClassNode(classAbi.className)] = nodeDependencies
    }

    fun deleteNode(className: String) {
        val node = ClassNode(className)
        val deleteNodeDependencies = graph[node] ?: return

        deleteNodeDependencies
            .filterIsInstance<AbiDependencyEdge.OutgoingEdge>()
            .forEach { edge ->
                graph[ClassNode(edge.dependentClass)]?.run {
                    removeAll { it is AbiDependencyEdge.IncomingEdge && it.dependsOnClass == className }
                }
            }
        graph.remove(node)
    }

    fun getClassesToRecompileOnClassChange(className: String): Set<String> {
        val node = ClassNode(className)
        val nodeDependencies = graph[node]

        return nodeDependencies?.detectClassesToRecompile(className)?.toSet() ?: emptySet()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun serialize(): ByteArray {
        return Cbor.encodeToByteArray(graph)
    }

    /**
     * Clears graph from all nodes.
     */
    fun clear() {
        graph.clear()
    }

    private fun addNewNode(classAbi: AbiReader.SourceFileAbi) {
        val node = ClassNode(classAbi.className)
        val edges = graph[node] ?: mutableSetOf()

        require(edges.filterIsInstance<AbiDependencyEdge.IncomingEdge>().isEmpty()) {
            "Failed to add, node for ${classAbi.className} is already exists"
        }

        fun createEdges(type: String, isPrivate: Boolean) {
            edges.add(
                AbiDependencyEdge.IncomingEdge(
                    isPrivate = isPrivate,
                    dependsOnClass = type
                )
            )

            val dependsOnNode = ClassNode(type)
            val dependsOnNodeEdges = graph[dependsOnNode] ?: mutableSetOf()

            dependsOnNodeEdges.add(
                AbiDependencyEdge.OutgoingEdge(
                    isPrivate = isPrivate,
                    dependentClass = classAbi.className
                )
            )
            graph[dependsOnNode] = dependsOnNodeEdges
        }

        classAbi
            .publicTypes
            .forEach { createEdges(it, false) }
        classAbi
            .privateTypes
            .forEach { createEdges(it, true) }

        graph[node] = edges
    }

    private fun Set<AbiDependencyEdge>.detectClassesToRecompile(
        className: String
    ): Set<String> {
        val accumulator = mutableSetOf<DependencyToRecompile>()

        detectRecompileDependenciesRecursively(
            className,
            accumulator,
        )

        return accumulator
            .fold(mutableSetOf<String>()) { acc, dependencyToRecompile ->
                acc.addAll(dependencyToRecompile.dependentTypes)
                acc
            }
            .filterNot { it == className }
            .toSet()
    }

    private fun Set<AbiDependencyEdge>.detectRecompileDependenciesRecursively(
        className: String,
        accumulator: MutableSet<DependencyToRecompile>,
    ) {
        val outgoingEdges = filterIsInstance<AbiDependencyEdge.OutgoingEdge>()

        accumulator.add(
            DependencyToRecompile(
                className,
                outgoingEdges.map { it.dependentClass }.toSet()
            )
        )

        outgoingEdges
            .filterNot { edge ->
                edge.isPrivate ||
                    accumulator.find { it.className == edge.dependentClass } != null
            }
            .forEach {
                graph[ClassNode(it.dependentClass)]?.detectRecompileDependenciesRecursively(
                    it.dependentClass,
                    accumulator
                )
            }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbiDependencyGraph

        if (graph != other.graph) return false

        return true
    }

    override fun hashCode(): Int {
        return graph.hashCode()
    }

    private data class DependencyToRecompile(
        val className: String,
        val dependentTypes: Set<String>
    )

    @Serializable
    internal data class ClassNode(
        val className: String
    )

    @Serializable
    internal sealed class AbiDependencyEdge {
        @Serializable
        data class OutgoingEdge(
            val isPrivate: Boolean,
            val dependentClass: String
        ) : AbiDependencyEdge()

        @Serializable
        data class IncomingEdge(
            val isPrivate: Boolean,
            val dependsOnClass: String
        ) : AbiDependencyEdge()
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        internal fun deserialize(
            cachedData: ByteArray
        ): AbiDependencyGraph = AbiDependencyGraph(
            Cbor.decodeFromByteArray(cachedData)
        )
    }
}
