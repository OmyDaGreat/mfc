plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.ajalt)
    implementation(libs.prefs)
    implementation(libs.kotlinx.coroutines)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useKotlinTest("2.1.21")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()
        minimize()
        archiveBaseName.set("mfc")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

application {
    mainClass = "xyz.malefic.mfc.RunnerKt"
}
