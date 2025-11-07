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

plugins {
    id("maven-publish")
    id("signing")
}

group = "org.bubenheimer.instancelimit"
version = "1.1.0-SNAPSHOT"

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
