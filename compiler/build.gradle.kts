plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":abi-reader"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.0.0")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.16.1")

    testImplementation("com.google.jimfs:jimfs:1.1")
}
