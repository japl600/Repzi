plugins {
    alias(libs.plugins.repzi.android.feature)
}

android {
    namespace = "com.japl.repzi.feature.widget"
}

dependencies {
    implementation(libs.androidx.glance.appwidget)

    androidTestImplementation(libs.androidx.glance.appwidget.testing)
}
