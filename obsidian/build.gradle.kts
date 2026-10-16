plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

// Obsidian — the Alarmzy design system as a standalone Compose library.
// Consume it from another app either as a source module (copy/include this folder) or via
// `./gradlew :obsidian:publishToMavenLocal` → implementation("dev.redfox:obsidian:<version>").
group = "dev.redfox"
version = "1.0.0"

android {
    namespace = "dev.redfox.obsidian"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    // Library resources (font, icons) are prefixed so they never clash with a host app's.
    resourcePrefix = "ob_"

    defaultConfig {
        minSdk = 31
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // Exposed as `api`: host apps write Compose/M3 code against the same versions, and
    // LocalHazeState / rememberHazeState come from Haze.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.material3)
    api(libs.haze)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            artifactId = "obsidian"
            afterEvaluate { from(components["release"]) }
        }
    }
}
