plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.everycue.core.attachments"
    compileSdk = 37
    defaultConfig { minSdk = 23 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

dependencies {
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
}
