plugins {
    alias(libs.plugins.spotless)
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

spotless {
    format("approvedKotlinBaseline") {
        target("app/expense-core/src/main/kotlin/com/subhrodip/pennywise/expensecore/categories/ExpenseCategory.kt")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

group = "com.subhrodip.pennywise"
version = "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
    apply(plugin = "jacoco")

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
