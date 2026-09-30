plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(25) }
dependencies {
    api(libs.boot.security)
    api(libs.boot.resource.server)
    testImplementation(libs.boot.test)
    testImplementation(libs.boot.web)
    testImplementation(libs.boot.webflux)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> { useJUnitPlatform() }
