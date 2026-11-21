plugins {
    alias(libs.plugins.repzi.android.library)
    alias(libs.plugins.repzi.android.compose)
}

android {
    namespace = "com.japl.repzi.core.ui"
}

dependencies {
    implementation(projects.core.domain)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    // Compose pulls in an older Espresso that crashes on recent Android versions (InputManager.getInstance).
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlin.test.junit)
}
