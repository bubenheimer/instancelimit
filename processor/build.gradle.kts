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
    id("base-conventions")
    id("jvm-conventions")
    alias(libs.plugins.ksp)
}

ksp {
    arg("instanceLimits.skipAnalyzerErrors", "false")
}

sourceSets {
    test {
        java {
            srcDir("src/test/data-java")
        }

        kotlin {
            srcDir("src/test/data-kotlin")
        }
    }
}

dependencies {
    implementation(project(":api"))

    implementation(libs.ksp.api)

    kspTest(project(":processor"))

    testImplementation(libs.kotlin.test)

    testImplementation(libs.junit)
}

extra["publicationComponent"] = "java"
