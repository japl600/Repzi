plugins {
    alias(libs.plugins.repzi.android.application)
    alias(libs.plugins.repzi.android.compose)
    alias(libs.plugins.repzi.android.hilt)
}

android {
    namespace = "com.japl.repzi"

    defaultConfig {
        applicationId = "com.japl.repzi"
        versionCode = 1
        versionName = "0.1.0"
    }

    // Builds res/xml/locales_config.xml from the values-XX folders, so the system offers a per-app language
    // setting (Android 13+). English is the default language (see res/resources.properties).
    androidResources {
        generateLocaleConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // El módulo app es la raíz de composición: Hilt necesita ver data y database para armar el grafo.
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.ui)

    implementation(projects.feature.exercises)
    implementation(projects.feature.routines)
    implementation(projects.feature.session)
    implementation(projects.feature.history)
    implementation(projects.feature.progress)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
}
