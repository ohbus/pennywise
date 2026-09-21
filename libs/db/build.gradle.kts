plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(25) }
dependencies {
    api(libs.boot.data.jpa)
    api(project(":libs:observability"))
    testImplementation(kotlin("test"))
}

tasks.withType<Test> { useJUnitPlatform() }
