import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ossrhUsername: String? by ext
val ossrhPassword: String? by ext

description = "A breadth-first search path finder"

plugins {
    `maven-publish`
    signing
    kotlin("jvm") version "1.4.0"
    id("me.champeau.gradle.jmh") version "0.5.2"
    id("org.jmailen.kotlinter") version "3.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    jmh("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    jmh("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.0")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
}

kotlin {
    explicitApi()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
    }
}

tasks.withType<Test> {
    failFast = true
    useJUnitPlatform()
}

jmh {
    profilers = listOf("stack")
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            if (project.version.toString().endsWith("SNAPSHOT")) {
                setUrl("https://oss.sonatype.org/content/repositories/snapshots")
            } else {
                setUrl("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            }
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("RSMod PathFinder")
                description.set(project.description)
                url.set("https://github.com/rsmod/pathfinder")
                inceptionYear.set("2021")

                licenses {
                    license {
                        name.set("ISC License")
                        url.set("https://opensource.org/licenses/isc-license.txt")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/rsmod/pathfinder.git")
                    developerConnection.set("scm:git:git@github.com:github.com/rsmod/pathfinder.git")
                    url.set("https://github.com/rsmod/pathfinder")
                }

                developers {
                    developer {
                        name.set("Tomm")
                        url.set("https://github.com/Tomm0017")
                    }
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}
