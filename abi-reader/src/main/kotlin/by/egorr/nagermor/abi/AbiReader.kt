package by.egorr.nagermor.abi

import java.nio.file.Path

interface AbiReader {
    fun parseSourceFileAbi(
        compiledSourceFile: Path
    ): SourceFileAbi

    data class SourceFileAbi(
        val className: String,
        val sourceFileName: String?,
        val privateTypes: List<String>,
        val publicTypes: List<String>
    )
}
