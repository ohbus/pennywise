plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}
kotlin { jvmToolchain(25) }
dependencies {
    implementation(project(":libs:db"))
    implementation(project(":libs:security"))
    implementation(libs.kotlin.reflect)
    implementation(project(":libs:errors"))
    implementation(project(":libs:ids"))
    implementation(libs.boot.actuator)
    runtimeOnly(libs.micrometer.prometheus)
    implementation(libs.boot.web)
    implementation(libs.boot.validation)
    implementation(libs.boot.security)
    implementation(libs.boot.resource.server)
    implementation(libs.boot.data.jpa)
    implementation(libs.boot.amqp)
    implementation(libs.boot.flyway)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
    implementation(libs.kotlinx.coroutines.core)
    testRuntimeOnly(libs.h2)
    testImplementation(libs.boot.test)
}
tasks.withType<Test> { useJUnitPlatform() }
base { archivesName.set("pennywise-expense-core") }
springBoot { mainClass.set("com.subhrodip.pennywise.expensecore.ExpenseCoreApplicationKt") }
