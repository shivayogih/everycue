plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.everycue.core.vision"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

dependencies {
    implementation(libs.google.play.services.mlkit.text.recognition)
    implementation(libs.google.play.services.code.scanner)
    testImplementation(libs.junit)
}

