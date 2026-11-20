plugins {
    alias(libs.plugins.repzi.android.library)
    alias(libs.plugins.repzi.android.hilt)
}

android {
    namespace = "com.japl.repzi.core.notifications"
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.process)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.kotlin.test.junit)
}
