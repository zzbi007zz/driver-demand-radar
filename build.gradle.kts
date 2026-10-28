import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.10"
    // Kotlin 2.x: Compose is a Kotlin compiler plugin (replaces the old androidx compose-compiler artifact)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
}

// Centralized versions (single module; no version catalog needed at this size)
// 2026.09.00 (compose 1.12.x) requires compileSdk 37 + AGP 9.1; 2026.06.01 is
// the newest BOM compatible with compileSdk 35 / AGP 8.13.
val composeBom = "2026.06.01"
val roomVersion = "2.8.5"

android {
    namespace = "com.february10.driverradar"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.february10.driverradar"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        // Inject the Maps key from gradle.properties as a resValue so the manifest can
        // reference @string/GOOGLE_MAPS_API_KEY. Providers keep it optional: missing or
        // empty yields an empty string, so a clean checkout still builds.
        resValue("string", "GOOGLE_MAPS_API_KEY", providers.gradleProperty("GOOGLE_MAPS_API_KEY").orNull.orEmpty())
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    // Kotlin 2.0+: Kotlin's JVM target is set on the Kotlin extension.
    // The Java task still uses android.compileOptions — pin it to 17 too so
    // the two compile tasks agree (AGP defaults javac to 1.8).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // Settings screen reads BuildConfig.VERSION_NAME
    }

}

dependencies {
    // Compose (BOM aligns all compose-* artifacts)
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")

    // Lifecycle / ViewModel for StateFlow-backed screens
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.4")

    // Room (KSP generates the DAO implementations)
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Location: FusedLocationProvider API for Phase 02.
    // (androidx.location:location:21.0.1 from the original scaffold does not
    //  exist on either Google or Maven Central — swapped to the play-services
    //  artifact, which is what FusedLocationProviderClient lives in.)
    implementation("com.google.android.gms:play-services-location:19.0.0")

    // Unit tests (JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.robolectric:robolectric:4.14.1")
}

// ---------------------------------------------------------------------------
// printSigningSha1
// Prints SHA-1 fingerprints for the debug keystore AND (if configured) the
// release keystore, so the user can apply GCP "API key restriction" correctly.
// Never fails when the release keystore is missing — it just says "not set".
// ---------------------------------------------------------------------------
tasks.register("printSigningSha1") {
    val keystoreJava = System.getenv("ANDROID_RELEASE_KEYSTORE")
    val keytool = file(System.getProperty("java.home")).parentFile.resolve("bin/keytool")

    doLast {
        fun sha1Of(keystore: File, alias: String, pass: String, label: String) {
            if (!keystore.exists()) {
                println("[signing:$label] not set (missing $keystore)")
                return
            }
            try {
                val out = Runtime.getRuntime().exec(
                    listOf(keytool.absolutePath, "-list", "-v",
                        "-keystore", keystore.absolutePath,
                        "-alias", alias, "-storepass", pass).toTypedArray()
                )
                out.inputStream.bufferedReader().forEachLine {
                    if (it.contains("SHA1:", true) || it.contains("SHA-1:", true)) println("[signing:$label] $it.trim()")
                }
                out.waitFor()
            } catch (e: Exception) {
                println("[signing:$label] could not read ($e)")
            }
        }
        val home = System.getProperty("user.home")
        sha1Of(file(home + "/.android/debug.keystore"), "androiddebugkey", "android", "debug")
        if (keystoreJava.isNullOrBlank()) {
            println("[signing:release] not set (ANDROID_RELEASE_KEYSTORE env missing)")
        } else {
            val pass = System.getenv("ANDROID_RELEASE_KEYSTORE_PASS") ?: "android"
            val alias = System.getenv("ANDROID_RELEASE_ALIAS") ?: "driverradar"
            sha1Of(file(keystoreJava), alias, pass, "release")
        }
    }
}
