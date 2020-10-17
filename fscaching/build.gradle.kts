plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler"))
    api("commons-codec:commons-codec:1.15")

    testImplementation("com.google.jimfs:jimfs:1.1")
}
