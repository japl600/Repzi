plugins {
    alias(libs.plugins.repzi.android.library)
    alias(libs.plugins.repzi.android.hilt)
}

android {
    namespace = "com.japl.repzi.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
