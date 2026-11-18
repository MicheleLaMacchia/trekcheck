import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("jacoco")
}

android {
    namespace = "it.trekcheck.app"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "it.trekcheck.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}

val coverageExcludes = listOf(
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*_Factory.*",
    "**/*_MembersInjector.*",
)

val debugKotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug")
val debugJavaClasses = layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
val debugUnitTestExecutionData = layout.buildDirectory.file(
    "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
)
val mainSources = files("src/main/java", "src/main/kotlin")
val mainClasses = files(
    fileTree(debugKotlinClasses) { exclude(coverageExcludes) },
    fileTree(debugJavaClasses) { exclude(coverageExcludes) },
)

val jacocoTestDebugUnitTestReport = tasks.register<JacocoReport>("jacocoTestDebugUnitTestReport") {
    dependsOn("testDebugUnitTest")
    executionData.setFrom(debugUnitTestExecutionData)
    sourceDirectories.setFrom(mainSources)
    classDirectories.setFrom(mainClasses)

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/debugUnitTest/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/debugUnitTest/report.xml"))
    }
}

val jacocoTestDebugUnitTestCoverageVerification = tasks.register<JacocoCoverageVerification>(
    "jacocoTestDebugUnitTestCoverageVerification",
) {
    dependsOn(jacocoTestDebugUnitTestReport)
    executionData.setFrom(debugUnitTestExecutionData)
    sourceDirectories.setFrom(mainSources)
    classDirectories.setFrom(mainClasses)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.05".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(jacocoTestDebugUnitTestCoverageVerification)
}
