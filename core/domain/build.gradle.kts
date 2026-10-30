plugins {
    alias(libs.plugins.repzi.jvm.library)
}

// Módulo Kotlin puro: no debe importar nada de android.*. Se mueve tal cual a KMP.
dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
