plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}
kotlin { jvmToolchain(25) }
dependencies {
    implementation(project(":libs:ids"))
    implementation(libs.boot.actuator)
    implementation(libs.boot.security)
    implementation(libs.boot.resource.server)
    implementation(libs.boot.graphql)
    implementation(libs.boot.webflux)
    implementation(libs.boot.webclient)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.boot.test)
}
tasks.withType<Test> { useJUnitPlatform() }
sourceSets {
    main {
        resources {
            srcDir(rootProject.file("contracts"))
        }
    }
}
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
base { archivesName.set("pennywise-bff") }
springBoot { mainClass.set("com.subhrodip.pennywise.bff.BffApplicationKt") }
