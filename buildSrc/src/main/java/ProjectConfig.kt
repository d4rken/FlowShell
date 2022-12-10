import com.android.build.gradle.LibraryExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import java.time.Instant

object ProjectConfig {
    const val packageName = "eu.darken.flowshell"

    const val minSdk = 26
    const val compileSdk = 33
    const val targetSdk = 33

    object Version {
        val major = 0
        val minor = 1
        val patch = 0
        val build = 0

        val name = "${major}.${minor}.${patch}-rc${build}"
        val code = major * 10000000 + minor * 100000 + patch * 1000 + build * 10
    }
}

fun lastCommitHash(): String = Runtime.getRuntime().exec("git rev-parse --short HEAD").let { process ->
    process.waitFor()
    val output = process.inputStream.use { input ->
        input.bufferedReader().use {
            it.readText()
        }
    }
    process.destroy()
    output.trim()
}

fun buildTime(): Instant = Instant.now()

/**
 * Configures the [kotlinOptions][org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions] extension.
 */
private fun LibraryExtension.kotlinOptions(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlinOptions", configure)

fun LibraryExtension.setupLibraryDefaults() {
    compileSdk = ProjectConfig.compileSdk

    defaultConfig {
        minSdk = ProjectConfig.minSdk
        targetSdk = ProjectConfig.targetSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-Xopt-in=kotlinx.coroutines.FlowPreview",
            "-Xopt-in=kotlin.time.ExperimentalTime",
            "-Xopt-in=kotlin.RequiresOptIn"
        )
    }

    packagingOptions {
        resources.excludes += "DebugProbesKt.bin"
    }
}