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
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    id("base-conventions")
    id("android-conventions")
}

android {
    namespace = "org.bubenheimer.instancelimit"

    buildFeatures {
        buildConfig = false
    }

    libs.versions.android.sdk.compile.get().let {
        it.toIntOrNull()?.let { compileSdk = it } ?: run { compileSdkPreview = it }
    }

    compileOptions {
        JavaVersion.toVersion(libs.versions.java.source.get()).let {
            sourceCompatibility = it
            targetCompatibility = it
        }
    }

    defaultConfig {
        minSdk = libs.versions.android.sdk.min.get().toInt()
    }

    publishing {
        singleVariant("debug") {
            withJavadocJar()
            withSourcesJar()
        }
    }
}

androidComponents {
    beforeVariants { if (it.buildType == "release") it.enable = false }
}

dependencies {
    implementation(project(":api"))

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    kspTest(project(":processor"))
}

extra["publicationComponent"] = "debug"
