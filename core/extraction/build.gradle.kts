plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.everycue.core.extraction"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

dependencies {
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
    testImplementation(libs.junit)
}
