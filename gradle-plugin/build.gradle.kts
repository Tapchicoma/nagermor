plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions {
        apiVersion = "1.3"
    }
}

gradlePlugin {
    (plugins) {
        register("nagermor") {
            id = "by.egorr.nagermor"
            implementationClass = "by.egorr.nagermor.gradle.NagermorPlugin"
        }
    }
}

dependencies {
    compileOnly(gradleApi())
    implementation(project(":compiler"))
}
