import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application") version "8.13.2"
    id("org.jetbrains.kotlin.android") version "2.2.10"
    // Kotlin 2.x: Compose is a Kotlin compiler plugin (replaces the old androidx compose-compiler artifact)
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    // Json DTOs for the Places API responses
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
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
    // bridges gms Task<T> -> suspend; GpsProvider's lastLocation.await() needs this
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Places API client (Phase 02): OkHttp for the REST calls +
    // kotlinx-serialization for the JSON DTOs (plugin declared above).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Unit tests (JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("org.robolectric:robolectric:4.14.1")
    // ApplicationProvider for a real (Robolectric) Context; runTest for the
    // suspend DAO calls in tests.
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

// ---------------------------------------------------------------------------
// printSigningSha1
// Prints SHA-1 fingerprints for the debug keystore AND (if configured) the
// release keystore, so the user can apply GCP "API key restriction" correctly.
// Never fails when the release keystore is missing — it just says "not set".
//
// All resolution happens inside doLast (via providers) so the task is
// configuration-cache compatible and portable: no script-level file() or
// System.getenv() captures, and the keytool path is discovered from
// java.home rather than assumed.
// ---------------------------------------------------------------------------
tasks.register("printSigningSha1") {
    doLast {
        // NOTE: this body sticks to pure static calls (System.*, File,
        // Runtime) on purpose. Referencing outer receivers (e.g. `providers`)
        // inside doLast captures the Gradle script object and breaks
        // configuration-cache serialization.
        val keytoolCandidates = listOf(
            File(System.getProperty("java.home"), "bin/keytool"),
            File("/usr/bin/keytool"),
        )
        var keytool: File? = null
        for (candidate in keytoolCandidates) {
            if (candidate.exists()) { keytool = candidate; break }
        }
        val keytoolPath = keytool?.absolutePath ?: error("keytool not found on this JVM")

        val home = System.getProperty("user.home")
        val release = System.getenv("ANDROID_RELEASE_KEYSTORE")
        val releasePass = System.getenv("ANDROID_RELEASE_KEYSTORE_PASS") ?: "android"
        val releaseAlias = System.getenv("ANDROID_RELEASE_ALIAS") ?: "driverradar"

        // Pairs of (label, keystore path, alias, storepass).
        val debugTarget = arrayOf("debug", "$home/.android/debug.keystore", "androiddebugkey", "android")
        val releaseTarget = arrayOf("release", release, releaseAlias, releasePass)
        val targets = if (release.isNullOrBlank()) arrayOf(debugTarget) else arrayOf(debugTarget, releaseTarget)

        for (target in targets) {
            val label = target[0]
            val keystore = File(target[1])
            if (!keystore.exists()) {
                println("[signing:$label] not set (missing $keystore)")
                continue
            }
            try {
                val out = Runtime.getRuntime().exec(
                    arrayOf(keytoolPath, "-list", "-v",
                        "-keystore", keystore.absolutePath,
                        "-alias", target[2], "-storepass", target[3])
                )
                val lines = out.inputStream.bufferedReader().readLines()
                for (line in lines) {
                    if (line.contains("SHA1:", true) || line.contains("SHA-1:", true)) {
                        println("[signing:$label] ${line.trim()}")
                    }
                }
                out.waitFor()
            } catch (e: Exception) {
                println("[signing:$label] could not read ($e)")
            }
        }
        if (release.isNullOrBlank()) {
            println("[signing:release] not set (ANDROID_RELEASE_KEYSTORE env missing)")
        }
    }
}
