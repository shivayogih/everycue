plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.everycue.core.recommendation"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

dependencies { testImplementation(libs.junit) }
