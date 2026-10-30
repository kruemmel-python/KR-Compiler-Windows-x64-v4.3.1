plugins {
    id("com.android.application")
}

android {
    namespace = "kr.compile.studio.demo4lang"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.compile.studio.demo4lang"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "4.3.1"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Automatischer KR 4-Sprachen-Vorlauf vor dem Android-Build:
tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(rootProject.tasks.named("compileKrUnifiedAndroidArm64"))
}
