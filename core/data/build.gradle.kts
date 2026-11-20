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
    implementation(projects.core.network)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(projects.core.testing)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
