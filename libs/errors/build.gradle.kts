plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
}
kotlin { jvmToolchain(25) }
dependencies {
    api(libs.boot.web)
    api(project(":libs:ids"))
    implementation(libs.boot.data.jpa)
}
