plugins {
    alias(libs.plugins.repzi.android.library)
    alias(libs.plugins.repzi.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.japl.repzi.core.network"
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
