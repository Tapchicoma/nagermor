plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    api(project(":compiler"))
    api("commons-codec:commons-codec:1.15")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.0.0")

    testImplementation("com.google.jimfs:jimfs:1.1")
}
