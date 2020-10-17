package by.egorr.nagermor.abi

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path

class JavaAbiReader : AbiReader {

    override fun parseSourceFileAbi(
        compiledSourceFile: Path
    ): AbiReader.SourceFileAbi {
        val classVisitor = JavaClassVisitor()
        val classReader = ClassReader(Files.newInputStream(compiledSourceFile))
        classReader.accept(classVisitor, 0)

        val publicTypes = classVisitor.publicTypes.toSet()
        val privateTypes = classVisitor.privateTypes.subtract(publicTypes).toSet()

        return AbiReader.SourceFileAbi(
            className = classVisitor.className,
            sourceFileName = classVisitor.sourceFileName,
            privateTypes = privateTypes,
            publicTypes = publicTypes
        )
    }

    private class JavaClassVisitor : ClassVisitor(Opcodes.ASM9) {
        var sourceFileName: String? = null
        lateinit var className: String
        val publicTypes = mutableSetOf<String>()
        val privateTypes = mutableSetOf<String>()

        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            className = name
            writeTypes(access.isPrivate()) { types ->
                if (superName != null) types.add(superName)

                interfaces?.forEach {
                    types.add(it)
                }
            }
        }

        override fun visitField(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            value: Any?
        ): FieldVisitor? {
            // TODO: Parse field annotations via FieldVisitor
            writeTypes(access.isPrivate()) {
                it.add(Type.getType(descriptor).className)
            }
            return null
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            // TODO: Parse method annotations via MethodVisitor
            writeTypes(access.isPrivate()) { types ->
                types.add(Type.getMethodType(descriptor).returnType.className)
                Type.getArgumentTypes(descriptor)?.forEach {
                    types.add(it.className)
                }
                exceptions?.forEach {
                    types.add(it)
                }
            }
            return JavaClassMethodVisitor(
                privateTypes
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
            sourceFileName = source
        }

        private inline fun writeTypes(
            isPrivate: Boolean,
            block: (MutableSet<String>) -> Unit
        ) {
            if (isPrivate) {
                block(privateTypes)
            } else {
                block(publicTypes)
            }
        }

        private fun Int.isPrivate(): Boolean = Modifier.isPrivate(this)
    }

    private class JavaClassMethodVisitor(
        private val privateTypes: MutableSet<String>
    ) : MethodVisitor(Opcodes.ASM9) {
        override fun visitTypeInsn(
            opcode: Int,
            type: String
        ) {
            privateTypes.add(type)
        }
    }
}
