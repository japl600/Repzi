plugins {
    alias(libs.plugins.repzi.jvm.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.test)
    api(libs.junit)
}
