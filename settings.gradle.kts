pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.4.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.4.10"
    }
}

include(":cli")
include(":fscaching")
include(":compiler")
include(":abi-reader")
include(":gradle-plugin")
