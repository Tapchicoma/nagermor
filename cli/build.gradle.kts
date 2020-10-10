plugins {
    application
    kotlin("jvm")
}

application {
    mainClassName = "by.egorr.nagermor.cli.CliKt"
    applicationName = "nagermor"
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.0.1")

    implementation(project(":fscaching"))
}
