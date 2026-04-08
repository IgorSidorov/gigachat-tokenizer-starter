import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

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
    total.html {
        onCheck.set(true)
    }

    verify {
        rule {
            disabled = false
            bound {
                coverageUnits = CoverageUnit.LINE
                minValue = 80
            }
            bound {
                coverageUnits = CoverageUnit.BRANCH
                minValue = 50
            }
        }
    }
}

val dokkaJavadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.named("dokkaGenerate"))
    from(layout.buildDirectory.dir("dokka/html"))
    archiveClassifier.set("javadoc")
}

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

            pom {
                name.set("GigaChat Tokenizer Starter")
                description.set("Spring Boot Starter for GigaChat tokenization (Ultra/Max models)")
                url.set("https://github.com/IgorSidorov/gigachat-tokenizer-starter")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("IgorSidorov")
                        name.set("Igor Sidorov")
                        email.set("igoryakha@list.ru")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/IgorSidorov/gigachat-tokenizer-starter.git")
                    developerConnection.set("scm:git:ssh://github.com:IgorSidorov/gigachat-tokenizer-starter.git")
                    url.set("https://github.com/IgorSidorov/gigachat-tokenizer-starter")
                }
            }
        }
    }
}