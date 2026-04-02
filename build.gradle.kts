import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import org.gradle.kotlin.dsl.invoke

plugins {
    alias(libs.plugins.spring)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    annotationProcessor(libs.spring.boot.configuration.processor)
    implementation(libs.djl.tokenizers)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.mockk)
}

springBoot {
    buildInfo()
}

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
    archiveClassifier.set("")
}

tasks.register("printVersion") {
    val projectVersion = project.version
    doLast {
        println(projectVersion)
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-noverify", "-XX:+EnableDynamicAgentLoading",
        "--add-opens", "java.base/java.time=ALL-UNNAMED"
    )
}

kotlin {
    jvmToolchain(21)
}

kover.reports {

    filters {
//        excludes.classes()
    }

    total.html {
        onCheck.set(true)
    }

    verify {
        rule {
//            disabled = false
            disabled = true
            bound {
                coverageUnits = CoverageUnit.LINE
                minValue = 80
            }
            bound {
                coverageUnits = CoverageUnit.BRANCH
                minValue = 60
            }
        }
    }
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaGenerate"))
    from(layout.buildDirectory.dir("dokka/html"))
    archiveClassifier.set("javadoc")
}

// 2. Создаем Jar с исходниками (обязательно для публикации)
val sourcesJar by tasks.registering(Jar::class) {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            artifact(sourcesJar)
            artifactId = "gigachat-tokenizer-starter"
        }
    }
}