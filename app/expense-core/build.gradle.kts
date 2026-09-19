plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}
kotlin { jvmToolchain(25) }
dependencies {
    implementation(libs.kotlin.reflect)
    implementation(project(":libs:errors"))
    implementation(project(":libs:ids"))
    implementation(libs.boot.actuator)
    implementation(libs.boot.web)
    implementation(libs.boot.validation)
    implementation(libs.boot.security)
    implementation(libs.boot.resource.server)
    implementation(libs.boot.data.jpa)
    implementation(libs.boot.amqp)
    implementation(libs.boot.flyway)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)
    testRuntimeOnly(libs.h2)
    testImplementation(libs.boot.test)
}
tasks.withType<Test> { useJUnitPlatform() }
base { archivesName.set("pennywise-expense-core") }
springBoot { mainClass.set("com.subhrodip.pennywise.expensecore.ExpenseCoreApplicationKt") }
