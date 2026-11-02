import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Módulo de feature: library + Compose + Hilt + dependencias de presentación. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("repzi.android.library")
            pluginManager.apply("repzi.android.compose")
            pluginManager.apply("repzi.android.hilt")

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:ui"))

                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-navigation-compose"))
                add("implementation", libs.library("androidx-hilt-navigation-compose"))
                add("implementation", libs.library("kotlinx-coroutines-android"))

                add("testImplementation", project(":core:testing"))
                // En módulos Android Gradle no infiere la variante JUnit de kotlin-test, hay que pedirla.
                add("testImplementation", libs.library("kotlin-test-junit"))
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("kotlinx-coroutines-test"))
                add("testImplementation", libs.library("turbine"))

                // Tests de pantallas Compose en dispositivo (el resto de dependencias las pone el plugin de Compose).
                add("androidTestImplementation", libs.library("androidx-test-ext-junit"))
                add("androidTestImplementation", libs.library("androidx-test-runner"))
                // Compose pulls in an older Espresso that crashes on recent Android versions (InputManager.getInstance).
                add("androidTestImplementation", libs.library("androidx-test-espresso-core"))
                add("androidTestImplementation", libs.library("kotlin-test-junit"))
            }
        }
    }
}
