package by.egorr.nagermor.abi

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.TypePath
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path

class JavaAbiReader(
    private val compiledFileExtension: String = "class"
) : AbiReader {

    override fun parseSourceFileAbi(
        compiledSourceFile: Path
    ): AbiReader.SourceFileAbi {
        val collector = Collector()
        val classVisitor = JavaClassVisitor(collector, compiledSourceFile, compiledFileExtension)
        val classReader = ClassReader(Files.newInputStream(compiledSourceFile))
        classReader.accept(classVisitor, 0)

        val publicTypes = collector.publicTypes.toSet()
        val privateTypes = collector.privateTypes.subtract(publicTypes).toSet()

        return AbiReader.SourceFileAbi(
            className = classVisitor.className,
            innerClassNames = collector.innerClasses,
            sourceFileName = collector.sourceFileName,
            privateTypes = privateTypes,
            publicTypes = publicTypes
        )
    }

    private class Collector {
        private val _publicTypes = mutableSetOf<String>()
        val publicTypes: Set<String> get() = _publicTypes.map(::cleanInnerClasses).toSet()

        private val _privateTypes = mutableSetOf<String>()
        val privateTypes: Set<String> get() = _privateTypes.map(::cleanInnerClasses).toSet()

        private val _innerClasses = mutableSetOf<String>()
        val innerClasses: Set<String> get() = _innerClasses.toSet()

        var sourceFileName: String? = null

        fun addInnerClassName(name: String) {
            _innerClasses.add(name)
        }

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

        private fun cleanInnerClasses(type: String): String = type.substringBefore('$')
    }

    private class JavaClassVisitor(
        private val collector: Collector,
        private val compiledSourceFile: Path,
        private val compiledFileExtension: String
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
                if (signature != null) {
                    readSignature(signature, collector.isPrivateAccess(access))
                }

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

            if (signature != null) {
                readSignature(signature, collector.isPrivateAccess(access))
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

                if (signature != null) {
                    readSignature(signature, collector.isPrivateAccess(access))
                }
            }

            return JavaClassMethodVisitor(
                collector,
                collector.isPrivateAccess(access)
            )
        }

        override fun visitInnerClass(
            name: String,
            outerName: String?,
            innerName: String?,
            access: Int
        ) {
            // For some reason inner class could visit itself here
            if (name == className ||
                collector.innerClasses.contains(className)) {
                return
            }

            val classReader = ClassReader(
                Files.newInputStream(
                    compiledSourceFile.resolveSibling("$name.$compiledFileExtension")
                )
            )
            collector.addInnerClassName(name)
            classReader.accept(
                JavaClassVisitor(
                    collector,
                    compiledSourceFile,
                    compiledFileExtension
                ),
                0
            )
        }

        override fun visitSource(
            source: String?,
            debug: String?
        ) {
            collector.sourceFileName = source
        }

        private fun readSignature(
            signature: String,
            isPrivate: Boolean
        ) {
            val reader = SignatureReader(signature)
            reader.accept(
                JavaClassGenericsVisitor(
                    collector,
                    isPrivate
                )
            )
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

    private class JavaClassGenericsVisitor(
        private val collector: Collector,
        private val isPrivate: Boolean
    ) : SignatureVisitor(Opcodes.ASM9) {
        override fun visitClassType(name: String) {
            collector.addType(isPrivate) {
                it.add(name)
            }
        }
    }
}
