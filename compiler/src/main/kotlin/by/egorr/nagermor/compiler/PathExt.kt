package by.egorr.nagermor.compiler

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Deletes given directory recursively with content inside.
 */
internal fun Path.deleteDirectoryWithContent() {
    if (Files.notExists(this) ||
        !Files.isDirectory(this)) return

    Files.walkFileTree(this, object : SimpleFileVisitor<Path>() {
        override fun postVisitDirectory(
            dir: Path,
            exc: IOException?
        ): FileVisitResult {
            Files.delete(dir)
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(
            file: Path,
            attrs: BasicFileAttributes
        ): FileVisitResult {
            Files.delete(file)
            return FileVisitResult.CONTINUE
        }
    })
}
