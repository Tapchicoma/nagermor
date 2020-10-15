import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf(
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    plugins.withId("kotlin") {
        dependencies {
            "testImplementation"(platform("org.junit:junit-bom:5.7.0"))
            "testImplementation"("org.junit.jupiter:junit-jupiter")
            "testImplementation"(kotlin("test-junit5"))
        }
    }
}
