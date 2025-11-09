/*
 * Copyright 2025 Uli Bubenheimer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.AndroidBasePlugin
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.Companion.fromTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

plugins {
    `maven-publish`
    signing
}

group = "org.bubenheimer.instancelimit"
version = "1.1.0-SNAPSHOT"

val libs = versionCatalogs.named("libs")

// Not leveraged by AGP
plugins.withType(JavaBasePlugin::class) {
    configure<JavaPluginExtension> {
        JavaVersion.toVersion(libs.findVersion("java.source").get().toString()).let {
            sourceCompatibility = it
            targetCompatibility = it
        }

        withJavadocJar()
        withSourcesJar()
    }
}

fun CommonExtension<*,*,*,*,*,*>.configureCommonDsl() {
    libs.findVersion("android.sdk.compile").get().toString().let {
        it.toIntOrNull()?.let { compileSdk = it } ?: run { compileSdkPreview = it }
    }

    with(compileOptions) {
        // AGP does not leverage the Java/JVM plugin, so must do this explicitly here
        JavaVersion.toVersion(libs.findVersion("java.source").get()).let {
            sourceCompatibility = it
            targetCompatibility = it
        }
    }

    with(defaultConfig) {
        minSdk = libs.findVersion("android.sdk.min").get().toString().toInt()
    }
}

plugins.withType(LibraryPlugin::class) {
    extensions.configure(LibraryExtension::class) {
        configureCommonDsl()
    }

    extensions.configure(LibraryAndroidComponentsExtension::class) {
        finalizeDsl { ->
            publishing {
                singleVariant("debug") {
                    withJavadocJar()
                    withSourcesJar()
                }
            }
        }
    }
}

plugins.withType(AndroidBasePlugin::class) {
    extensions.configure(AndroidComponentsExtension::class) {
        beforeVariants { if (it.buildType == "release") it.enable = false }
    }
}

plugins.withType(KotlinBasePlugin::class) {
    configure<KotlinProjectExtension> {
        jvmToolchain(libs.findVersion("java.toolchain").get().toString().toInt())

        explicitApi()

        // Cast is valid for both KotlinJvmProjectExtension and KotlinAndroidProjectExtension,
        // which is all we care about; crash otherwise to flag invalid assumptions
        @Suppress("UNCHECKED_CAST")
        (this as HasConfigurableKotlinCompilerOptions<KotlinJvmCompilerOptions>).compilerOptions
            .apply {
                jvmTarget = fromTarget(libs.findVersion("java.source").get().toString())
                progressiveMode = true
                jvmDefault = NO_COMPATIBILITY
                verbose = false
                extraWarnings = true
                freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
            }
    }
}

publishing {
    publications {
        register<MavenPublication>(name = "library") {
            val publicationComponent: String by extra

            afterEvaluate { // lets projects set the extra first
                from(components[publicationComponent])
            }

            pom {
                name = "${project.group}:${project.name}"
                description = "Annotations for Android StrictMode setClassInstanceLimit()"
                url = "https://github.com/bubenheimer/instancelimit/"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        name = "Uli Bubenheimer"
                        organizationUrl = "https://github.com/bubenheimer/"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/bubenheimer/instancelimit.git"
                    developerConnection = "scm:git:ssh://github.com:bubenheimer/instancelimit.git"
                    url = "http://github.com/bubenheimer/instancelimit/tree/master"
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) {
                    "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                } else {
                    "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                }
            )

            credentials {
                username = project.properties["sonatypeUsername"].toString()
                password = project.properties["sonatypePassword"].toString()
            }
        }
    }

    signing {
        useGpgCmd()

        sign(publishing.publications["library"])
    }
}
