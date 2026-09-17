plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.dependency.management)
}

kotlin { jvmToolchain(25) }

dependencies {
    api(libs.boot.actuator)
    compileOnly("org.slf4j:slf4j-api")
    testImplementation(libs.boot.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> { useJUnitPlatform() }
