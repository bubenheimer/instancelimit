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

import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.Companion.fromTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin

val libs = versionCatalogs.named("libs")

project.plugins.withType(KotlinBasePlugin::class) {
    with(project.kotlinExtension) {
        jvmToolchain(libs.findVersion("java.toolchain").get().toString().toInt())

        explicitApi()

        val compilerOptions: KotlinJvmCompilerOptions = when (this) {
            is KotlinJvmProjectExtension -> compilerOptions
            is KotlinAndroidProjectExtension -> compilerOptions
            else -> error("")
        }

        with(compilerOptions) {
            jvmTarget = fromTarget(libs.findVersion("java.source").get().toString())
            progressiveMode = true
            jvmDefault = NO_COMPATIBILITY
            verbose = false
            extraWarnings = true
            freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
        }
    }
}
