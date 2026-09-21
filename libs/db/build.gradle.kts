plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(25) }
dependencies {
    compileOnly(libs.boot.data.jpa)
    compileOnly(libs.boot.web)
    api(project(":libs:observability"))
    testImplementation(kotlin("test"))
    testImplementation(libs.boot.web)
    testImplementation(libs.boot.test)
}

tasks.withType<Test> { useJUnitPlatform() }
