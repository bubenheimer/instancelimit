/*
 * Copyright 2023 Uli Bubenheimer
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

@file:JvmName("InstanceLimits")

package org.bubenheimer.instancelimit

import android.os.StrictMode.VmPolicy.Builder
import android.os.StrictMode.getVmPolicy
import android.os.StrictMode.setVmPolicy
import java.util.ServiceLoader

public fun setInstanceLimits(classLoader: ClassLoader): Unit =
    apply { sequences(classLoader) }

public fun setNonReflectiveInstanceLimits(): Unit =
    apply { nonReflectiveSequence() }

public fun setReflectiveInstanceLimits(classLoader: ClassLoader): Unit =
    apply { reflectiveSequence(classLoader) }

public fun Builder.setInstanceLimits(classLoader: ClassLoader): Builder =
    apply { sequences(classLoader) }

public fun Builder.setNonReflectiveInstanceLimits(): Builder =
    apply { nonReflectiveSequence() }

public fun Builder.setReflectiveInstanceLimits(classLoader: ClassLoader): Builder =
    apply { reflectiveSequence(classLoader) }

private val serviceLoader: ServiceLoader<InstanceLimitsProvider> =
    ServiceLoader.load(InstanceLimitsProvider::class.java)

private fun InstanceLimitsProvider.sequences(classLoader: ClassLoader) =
    nonReflectiveSequence() + reflectiveSequence(classLoader)

private fun InstanceLimitsProvider.nonReflectiveSequence() =
    nonReflective.asSequence().map { (cls, instanceLimit) -> cls.java to instanceLimit }

private fun InstanceLimitsProvider.reflectiveSequence(classLoader: ClassLoader) =
    reflective.asSequence().map { (className, instanceLimit) ->
        Class.forName(className, false, classLoader) to instanceLimit
    }

private inline fun apply(block: InstanceLimitsProvider.() -> Sequence<Pair<Class<*>, Int>>) =
    setVmPolicy(Builder(getVmPolicy()).apply(block).build())

private inline fun Builder.apply(
    block: InstanceLimitsProvider.() -> Sequence<Pair<Class<*>, Int>>
): Builder {
    serviceLoader.forEach {
        it.block().forEach { (cls, instanceLimit) ->
            setClassInstanceLimit(cls, instanceLimit)
        }
    }

    return this
}
