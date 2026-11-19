plugins {
    alias(libs.plugins.repzi.android.feature)
}

android {
    namespace = "com.japl.repzi.feature.settings"
}

dependencies {
    // `Libs` is part of the public API of the licenses screen, which receives the library list from :app.
    api(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
}
