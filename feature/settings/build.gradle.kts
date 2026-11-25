plugins {
    alias(libs.plugins.repzi.android.feature)
}

android {
    namespace = "com.japl.repzi.feature.settings"

    testOptions {
        // The preview engine needs the compiled resources to resolve themes and strings.
        unitTests.isIncludeAndroidResources = true

        screenshotTests.create("screenshotTest") {
            engineVersion = "0.0.1-alpha17"
            targetVariants.add("debug")

            dependencies {
                implementation(libs.androidx.compose.ui.tooling)
                implementation("com.android.tools.screenshot:screenshot-validation-api:0.0.1-alpha17")
            }
        }
    }
}

dependencies {
    // `Libs` is part of the public API of the licenses screen, which receives the library list from :app.
    api(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
}

androidComponents {
    beforeVariants { variantBuilder ->
        (variantBuilder as? com.android.build.api.variant.HasHostTestsBuilder)
            ?.hostTests?.get(com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE)?.enable = true
    }
}
