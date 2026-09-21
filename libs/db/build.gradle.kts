plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(25) }
dependencies {
    api(libs.boot.data.jpa)
    api(libs.boot.web)
    api(project(":libs:observability"))
    testImplementation(kotlin("test"))
}

tasks.withType<Test> { useJUnitPlatform() }
