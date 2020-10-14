plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":abi-reader"))

    testImplementation("com.google.jimfs:jimfs:1.1")
}
