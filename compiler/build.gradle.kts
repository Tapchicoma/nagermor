plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    implementation(project(":abi-reader"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.0.0")

    testImplementation("com.google.jimfs:jimfs:1.1")
}
