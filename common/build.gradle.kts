import org.jetbrains.compose.compose
import com.arstagaev.gradle.Deps

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.library")
    kotlin("plugin.serialization") version "1.9.0"
    id("kotlin-parcelize")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    android()
    jvm("desktop") {
        jvmToolchain(11)
    }
    val serialization_version = "1.5.1"

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                // Works as common dependency as well as the platform one
                implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
                val decompose = "2.0.0"
                implementation("com.arkivanov.decompose:decompose:${decompose}")
                implementation("com.arkivanov.decompose:extensions-compose-jetbrains:${decompose}")

                implementation(Deps.Coroutines.core)
                implementation(Deps.Ktor.core)
                implementation(Deps.Ktor.clientContentNegotiation)
                implementation(Deps.Ktor.serializationKotlinxJson)
                implementation(Deps.Ktor.clientLogging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {

                api("androidx.appcompat:appcompat:1.5.1")
                api("androidx.core:core-ktx:1.9.0")
            }
        }
//        val androidTest by getting {
//            dependencies {
//                implementation("junit:junit:4.13.2")
//            }
//        }
        val desktopMain by getting {
            dependencies {
                api(compose.preview)
            }
        }
        val desktopTest by getting
    }
}

android {
    compileSdkVersion(33)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(33)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}