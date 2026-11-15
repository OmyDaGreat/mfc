plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.graalvm.native)
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
            useKotlinTest()
        }
    }
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "xyz.malefic.mfc.RunnerKt"
    applicationName = "mfc"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

graalvmNative {
    toolchainDetection = true

    binaries.named("main") {
        imageName = "mfc"
        mainClass = application.mainClass
        javaLauncher =
            javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(25)
                vendor = JvmVendorSpec.GRAAL_VM
                nativeImageCapable = true
            }
        buildArgs.addAll("--enable-native-access=ALL-UNNAMED", "--enable-url-protocols=http,https")
    }
}
