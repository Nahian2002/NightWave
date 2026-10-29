import java.security.MessageDigest
import java.nio.file.Files
import java.util.concurrent.TimeUnit

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// NW126: source identity is derived from actual input bytes, not a version label.
// No timestamp, workstation path, account, branch name or remote URL is embedded.
fun nw126Sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes).joinToString("") { "%02X".format(it.toInt() and 255) }

fun nw126Inputs(root: File): List<File> {
    val result = mutableListOf<File>()
    fun scan(folder: File, include: (File) -> Boolean) {
        check(folder.isDirectory) { "Missing source directory" }
        folder.walkTopDown().onEnter {
            check(!Files.isSymbolicLink(it.toPath())) { "Linked source directories are unsupported" }
            true
        }.forEach { file ->
            check(!Files.isSymbolicLink(file.toPath())) { "Linked source files are unsupported" }
            if (file.isFile && include(file)) result += file
        }
    }
    scan(File(root, "app/src")) { true }
    scan(File(root, "tools")) { it.extension in setOf("ps1", "py", "cmd", "kt") }
    listOf("app/build.gradle.kts", "app/proguard-rules.pro", "build.gradle.kts",
        "settings.gradle.kts", "gradle.properties", "build-windows.cmd").forEach {
        result += File(root, it).also { input -> check(input.isFile) { "Missing build configuration" } }
    }
    return result.distinct().sortedBy { it.relativeTo(root).invariantSeparatorsPath }
}

fun nw126Revision(root: File): String {
    // Revision is context only. The source digest also covers uncommitted inputs.
    fun git(vararg args: String): String? = runCatching {
        val process = ProcessBuilder(listOf("git", "-C", root.absolutePath) + args)
            .redirectErrorStream(true).start()
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            null
        } else if (process.exitValue() == 0) process.inputStream.bufferedReader().readText().trim() else null
    }.getOrNull()
    val top = git("rev-parse", "--show-toplevel") ?: return "unavailable"
    if (File(top).canonicalFile != root.canonicalFile) return "unavailable"
    return git("rev-parse", "HEAD")?.takeIf { it.matches(Regex("[0-9a-f]{40}")) } ?: "unavailable"
}

android {
    namespace = "com.nightwave.player"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nightwave.player"
        minSdk = 26
        targetSdk = 36
        versionCode = 35
        versionName = "2.0.0-alpha5s"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { viewBinding = true; compose = true }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Per-variant assets are generated outside app/src; there is no self-hashing file.
android.buildTypes.forEach { type ->
    val capitalized = type.name.replaceFirstChar { it.uppercaseChar() }
    val generated = layout.buildDirectory.dir("generated/nightwaveProvenance/${type.name}/assets")
    val provenance = tasks.register("generateNightWave${capitalized}Provenance") {
        outputs.dir(generated)
        outputs.upToDateWhen { false }
        doLast {
            val root = rootProject.projectDir
            val inputs = nw126Inputs(root)
            check(inputs.size >= 50) { "Incomplete source tree" }
            val manifest = inputs.joinToString("") { file ->
                nw126Sha(file.readBytes()) + "  " + file.relativeTo(root).invariantSeparatorsPath + "\n"
            }
            val digest = nw126Sha(("NightWave-source-v1\n" + manifest).toByteArray(Charsets.UTF_8))
            val directory = generated.get().asFile.also { it.mkdirs() }
            File(directory, "nightwave-build.json").writeText("""{
              "schema": 1,
              "algorithm": "NightWave-source-v1",
              "variant": "${type.name}",
              "sourceSha256": "$digest",
              "inputCount": ${inputs.size},
              "revisionContext": "${nw126Revision(root)}"
            }
            """.trimIndent() + "\n", Charsets.UTF_8)
        }
    }
    android.sourceSets.getByName(type.name).assets.srcDir(generated)
    tasks.matching { it.name == "merge${capitalized}Assets" }.configureEach { dependsOn(provenance) }
}

dependencies {
    // Shadow persistence: legacy collections remain authoritative until migration QA.
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("androidx.work:work-runtime-ktx:2.10.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    androidTestImplementation("androidx.room:room-testing:2.7.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    // v2 Compose is added in parallel; the legacy playback UI remains available.
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("io.coil-kt:coil-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // NW129: createComposeRule requires a debug-only host Activity manifest.
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.6.1")
    implementation("androidx.media3:media3-session:1.6.1")
    // Explicit Guava API used by non-blocking Media3 widget/session commands.
    implementation("com.google.guava:guava:33.3.1-android")
    implementation("androidx.media3:media3-datasource:1.6.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
