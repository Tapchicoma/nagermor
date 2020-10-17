package by.egorr.nagermor.compiler.incremental

import com.github.javaparser.StaticJavaParser
import java.nio.file.Path

/**
 * Parse java source file and get top level class/interface/enum/annotation name.
 */
internal fun Path.getJavaSourceFileTopElementName(
    outputFormatter: (packageName: String?, className: String) -> String = classNameFormatter
): String {
    val parsedSource = StaticJavaParser.parse(this)

    val packageDeclaration = parsedSource.packageDeclaration
    val packageName = if (packageDeclaration.isPresent) {
        packageDeclaration.get().nameAsString
    } else {
        null
    }

    val classFileName = fileName.toString().substringBeforeLast('.')
    var className: String? = null

    val classDeclaration = parsedSource.getClassByName(classFileName)
    if (classDeclaration.isPresent) className = classDeclaration.get().nameAsString

    if (className == null) {
        val interfaceDeclaration = parsedSource.getInterfaceByName(classFileName)
        if (interfaceDeclaration.isPresent) className = interfaceDeclaration.get().nameAsString
    }

    if (className == null) {
        val enumDeclaration = parsedSource.getEnumByName(classFileName)
        if (enumDeclaration.isPresent) className = enumDeclaration.get().nameAsString
    }

    if (className == null) {
        val annotationDeclaration = parsedSource.getAnnotationDeclarationByName(classFileName)
        if (annotationDeclaration.isPresent) className = annotationDeclaration.get().nameAsString
    }

    requireNotNull(className) {
        "Could not find class name for $this source file."
    }

    return outputFormatter(packageName, className)
}

internal val classNameFormatter: (packageName: String?, className: String) -> String = { packageName, className ->
    if (packageName.isNullOrBlank()) {
        className
    } else {
        "$packageName.$className"
    }.replace('.', '/')
}
