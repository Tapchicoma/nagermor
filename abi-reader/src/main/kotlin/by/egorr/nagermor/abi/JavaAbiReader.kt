package by.egorr.nagermor.abi

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path

class JavaAbiReader : AbiReader {

    override fun parseSourceFileAbi(
        compiledSourceFile: Path
    ): AbiReader.SourceFileAbi {
        val collector = Collector()
        val classVisitor = JavaClassVisitor(collector)
        val classReader = ClassReader(Files.newInputStream(compiledSourceFile))
        classReader.accept(classVisitor, 0)

        val publicTypes = collector.publicTypes.toSet()
        val privateTypes = collector.privateTypes.subtract(publicTypes).toSet()

        return AbiReader.SourceFileAbi(
            className = classVisitor.className,
            sourceFileName = collector.sourceFileName,
            privateTypes = privateTypes,
            publicTypes = publicTypes
        )
    }

    private class Collector {
        private val _publicTypes = mutableSetOf<String>()
        val publicTypes: Set<String> get() = _publicTypes.toSet()

        private val _privateTypes = mutableSetOf<String>()
        val privateTypes: Set<String> get() = _privateTypes.toSet()

        var sourceFileName: String? = null

        fun addType(
            access: Int,
            block: (MutableSet<String>) -> Unit
        ) = addType(isPrivateAccess(access), block)

        fun addType(
            isPrivate: Boolean,
            block: (MutableSet<String>) -> Unit
        ) {
            if (isPrivate) {
                block(_privateTypes)
            } else {
                block(_publicTypes)
            }
        }

        fun isPrivateAccess(
            access: Int
        ): Boolean = Modifier.isPrivate(access)

        fun getObjectInternalName(
            descriptor: String
        ): String = Type.getType(descriptor).internalName
    }

    private class JavaClassVisitor(
        private val collector: Collector
    ) : ClassVisitor(Opcodes.ASM9) {
        private var isClassPrivate: Boolean = true
        lateinit var className: String

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            className = name
            isClassPrivate = collector.isPrivateAccess(access)
            collector.addType(access) { types ->
                if (superName != null) types.add(superName)

                interfaces?.forEach {
                    types.add(it)
                }
            }
        }

        override fun visitAnnotation(
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor? {
            if (visible) {
                collector.addType(isClassPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }

            return JavaClassAnnotationVisitor(
                collector,
                isClassPrivate
            )
        }

        override fun visitTypeAnnotation(
            typeRef: Int,
            typePath: TypePath?,
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor? {
            if (visible) {
                collector.addType(isClassPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }
            return JavaClassAnnotationVisitor(
                collector,
                isClassPrivate
            )
        }

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?
        ): FieldVisitor? {
            collector.addType(access) {
                it.add(collector.getObjectInternalName(descriptor))
            }

            return JavaClassFieldVisitor(
                collector,
                collector.isPrivateAccess(access)
            )
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            collector.addType(access) { types ->
                types.add(Type.getMethodType(descriptor).returnType.internalName)
                Type.getArgumentTypes(descriptor)?.forEach {
                    types.add(it.internalName)
                }
                exceptions?.forEach {
                    types.add(it)
                }
            }

            return JavaClassMethodVisitor(
                collector,
                collector.isPrivateAccess(access)
            )
        }

        override fun visitInnerClass(
            name: String?,
            outerName: String?,
            innerName: String?,
            access: Int
        ) {
            // TODO: Parse inner classes
        }

        override fun visitSource(
            source: String?,
            debug: String?
        ) {
            collector.sourceFileName = source
        }
    }

    private class JavaClassMethodVisitor(
        private val collector: Collector,
        private val isMethodPrivate: Boolean
    ) : MethodVisitor(Opcodes.ASM9) {

        override fun visitAnnotation(
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor? {
            if (visible) {
                collector.addType(isMethodPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }
            return JavaClassAnnotationVisitor(
                collector,
                isMethodPrivate
            )
        }

        override fun visitTypeAnnotation(
            typeRef: Int,
            typePath: TypePath?,
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor {
            if (visible) {
                collector.addType(isMethodPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }

            return JavaClassAnnotationVisitor(
                collector,
                isMethodPrivate
            )
        }

        override fun visitParameterAnnotation(
            parameter: Int,
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor {
            if (visible) {
                collector.addType(isMethodPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }

            return JavaClassAnnotationVisitor(
                collector,
                isMethodPrivate
            )
        }

        override fun visitTypeInsn(
            opcode: Int,
            type: String
        ) {
            collector.addType(true) {
                it.add(type)
            }
        }
    }

    private class JavaClassFieldVisitor(
        private val collector: Collector,
        private val isFieldPrivate: Boolean
    ) : FieldVisitor(Opcodes.ASM9) {
        override fun visitAnnotation(
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor? {
            if (visible) {
                collector.addType(isFieldPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }
            return JavaClassAnnotationVisitor(
                collector,
                isFieldPrivate
            )
        }

        override fun visitTypeAnnotation(
            typeRef: Int,
            typePath: TypePath?,
            descriptor: String,
            visible: Boolean
        ): AnnotationVisitor? {
            if (visible) {
                collector.addType(isFieldPrivate) {
                    it.add(collector.getObjectInternalName(descriptor))
                }
            }

            return JavaClassAnnotationVisitor(
                collector,
                isFieldPrivate
            )
        }
    }

    private class JavaClassAnnotationVisitor(
        private val collector: Collector,
        private val isPrivate: Boolean
    ) : AnnotationVisitor(Opcodes.ASM9) {

        override fun visitEnum(
            name: String?,
            descriptor: String,
            value: String
        ) {
            collector.addType(isPrivate) {
                it.add(collector.getObjectInternalName(descriptor))
            }
        }

        override fun visitAnnotation(
            name: String,
            descriptor: String
        ): AnnotationVisitor? {
            collector.addType(isPrivate) {
                it.add(collector.getObjectInternalName(descriptor))
            }

            return JavaClassAnnotationVisitor(collector, isPrivate)
        }

        override fun visitArray(
            name: String
        ): AnnotationVisitor? {
            return JavaClassAnnotationVisitor(collector, isPrivate)
        }
    }
}
