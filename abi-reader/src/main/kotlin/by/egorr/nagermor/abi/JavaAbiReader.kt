package by.egorr.nagermor.abi

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.ModuleVisitor
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

        return AbiReader.SourceFileAbi(
            className = classVisitor.className,
            sourceFileName = classVisitor.sourceFileName,
            privateTypes = classVisitor.privateTypes.filter(::filterJvmClasses).toList(),
            publicTypes = classVisitor.publicTypes.filter(::filterJvmClasses).toList()
        )
    }

    private fun filterJvmClasses(type: String): Boolean = when {
        type.startsWith("java/lang") -> false
        else -> true
    }

    private class JavaClassVisitor : ClassVisitor(Opcodes.ASM9) {
        var sourceFileName: String? = null
        lateinit var className: String
        val publicTypes = mutableListOf<String>()
        val privateTypes = mutableListOf<String>()

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
            name: String?,
            descriptor: String?,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            System.err.println("Visiting field: $name")
            return super.visitField(access, name, descriptor, signature, value)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            System.err.println("Visiting method: $name, signature: $signature, exceptions: $exceptions")
            return null
        }

        override fun visitInnerClass(
            name: String?,
            outerName: String?,
            innerName: String?,
            access: Int
        ) {
            System.err.println(
                "Visiting inner class $name, outer name: $outerName, inner name: $innerName, is private: ${access.isPrivate()}"
            )
        }

        override fun visitSource(
            source: String?,
            debug: String?
        ) {
            sourceFileName = source
        }

        private inline fun writeTypes(
            isPrivate: Boolean,
            block: (MutableList<String>) -> Unit
        ) {
            if (isPrivate) {
                block(privateTypes)
            } else {
                block(publicTypes)
            }
        }

        private fun Int.isPrivate(): Boolean = Modifier.isPrivate(this)
    }
}
