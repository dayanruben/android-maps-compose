// buildSrc/src/main/kotlin/PublishingConventionPlugin.kt
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.plugins.signing.SigningExtension
import org.gradle.api.publish.maven.*

class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {

            applyPlugins()
            configureJacoco()
            configurePublishing()
            configureSigning()
        }
    }

    private fun Project.applyPlugins() {
        apply(plugin = "com.android.library")
        apply(plugin = "com.mxalbert.gradle.jacoco-android")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "signing")
    }

    private fun Project.configureJacoco() {
        configure<JacocoPluginExtension> {
            toolVersion = "0.8.7"

        }

        tasks.withType<Test>().configureEach {
            extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }

    private fun Project.configurePublishing() {
        extensions.configure<com.android.build.gradle.LibraryExtension> {
            publishing {
                singleVariant("release") {
                    withSourcesJar()
                    withJavadocJar()
                }
            }
        }
        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("aar") {
                    afterEvaluate {
                        from(components["release"])
                    }
                    pom {
                        name.set(project.name)
                        description.set("Jetpack Compose components for the Maps SDK for Android")
                        url.set("https://github.com/googlemaps/android-maps-compose")
                        scm {
                            connection.set("scm:git@github.com:googlemaps/android-maps-compose.git")
                            developerConnection.set("scm:git@github.com:googlemaps/android-maps-compose.git")
                            url.set("https://github.com/googlemaps/android-maps-compose")
                        }
                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                distribution.set("repo")
                            }
                        }
                        organization {
                            name.set("Google Inc")
                            url.set("http://developers.google.com/maps")
                        }
                        developers {
                            developer {
                                name.set("Google Inc.")
                            }
                        }
                    }
                }
            }
            repositories {
                maven {
                    val releasesRepoUrl =
                        uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                    val snapshotsRepoUrl =
                        uri("https://central.sonatype.com/repository/maven-snapshots/")
                    url = if (project.version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
                    credentials {
                        username = project.findProperty("sonatypeToken") as String?
                        password = project.findProperty("sonatypeTokenPassword") as String?
                    }
                }
            }
        }
    }

    private fun Project.configureSigning() {
        configure<SigningExtension> {
            sign(extensions.getByType<PublishingExtension>().publications["aar"])
        }
    }
}
