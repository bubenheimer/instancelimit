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

package org.bubenheimer.instancelimit

import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.os.StrictMode.VmPolicy.Builder
import io.mockk.Ordering.ORDERED
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.just
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import kotlin.reflect.KClass
import kotlin.test.Test

internal class InstanceLimitsTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @RelaxedMockK
    private lateinit var vmPolicyBuilder: Builder

    @Test
    fun testNonReflective() {
        vmPolicyBuilder.setNonReflectiveInstanceLimits()

        verifyNonReflective()

        confirmVerified(vmPolicyBuilder)
    }

    @Test
    fun testReflective() {
        vmPolicyBuilder.setReflectiveInstanceLimits(classLoader)

        verifyReflective()

        confirmVerified(vmPolicyBuilder)
    }

    @Test
    fun testBoth() {
        vmPolicyBuilder.setInstanceLimits(classLoader)

        verifyNonReflective()
        verifyReflective()

        confirmVerified(vmPolicyBuilder)
    }

    private fun verifyNonReflective() {
        verify(exactly = 1) {
            setClassInstanceLimit(MyPublic::class, 1)
            setClassInstanceLimit(MyInternal::class, 2)
        }
    }

    private fun verifyReflective() {
        verify(exactly = 1) {
            setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate", 3)
            setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate\$MyInner", 4)
            setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate\$MyNested", 5)
        }
    }

    private fun setClassInstanceLimit(cls: KClass<*>, limit: Int) {
        vmPolicyBuilder.setClassInstanceLimit(cls.java, limit)
    }

    private fun setClassInstanceLimit(name: String, limit: Int) {
        vmPolicyBuilder.setClassInstanceLimit(name, limit)
    }
}

internal class InstanceLimitsStaticTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @MockK
    private lateinit var vmPolicy: VmPolicy

    @MockK
    private lateinit var vmPolicyBuilder: Builder

    @Before
    fun setUp() {
        mockkStatic(StrictMode::class)
        every { StrictMode.getVmPolicy() } returns vmPolicy
        every { StrictMode.setVmPolicy(vmPolicy) } just runs
        mockkConstructor(Builder::class)
        every { anyConstructed<Builder>().setClassInstanceLimit(any(), any()) } returns vmPolicyBuilder
        every { anyConstructed<Builder>().build() } returns vmPolicy
    }

    @After
    fun tearDown() {
        unmockkConstructor(Builder::class)
        unmockkStatic(StrictMode::class)
    }

    @Test
    fun testNonReflective() {
        setNonReflectiveInstanceLimits()

        verify(ORDERED) {
            StrictMode.getVmPolicy()
            anyConstructed<Builder>().setClassInstanceLimit(any(), any())
            anyConstructed<Builder>().build()
            StrictMode.setVmPolicy(vmPolicy)
        }

        verify(exactly = 1) {
            StrictMode.getVmPolicy()
            anyConstructed<Builder>().setClassInstanceLimit(MyPublic::class.java, 1)
            anyConstructed<Builder>().setClassInstanceLimit(MyInternal::class.java, 2)
            anyConstructed<Builder>().build()
            StrictMode.setVmPolicy(vmPolicy)
        }

        confirmVerified(vmPolicyBuilder, vmPolicy)
    }

    @Test
    fun testReflective() {
        setReflectiveInstanceLimits(classLoader)

        verify(ORDERED) {
            StrictMode.getVmPolicy()
            anyConstructed<Builder>().setClassInstanceLimit(any(), any())
            anyConstructed<Builder>().build()
            StrictMode.setVmPolicy(vmPolicy)
        }

        verify(exactly = 1) {
            StrictMode.getVmPolicy()
            anyConstructed<Builder>().setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate", 3)
            anyConstructed<Builder>().setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate\$MyInner", 4)
            anyConstructed<Builder>().setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate\$MyNested", 5)
            anyConstructed<Builder>().build()
            StrictMode.setVmPolicy(vmPolicy)
        }

        confirmVerified(vmPolicyBuilder, vmPolicy)
    }

    @Test
    fun testBoth() {
        setInstanceLimits(classLoader)

        verify(ORDERED) {
            StrictMode.getVmPolicy()
            anyConstructed<Builder>().setClassInstanceLimit(any(), any())
            anyConstructed<Builder>().build()
            StrictMode.setVmPolicy(vmPolicy)
        }

        verify(exactly = 1) {
            StrictMode.getVmPolicy()
            anyConstructed<Builder>().setClassInstanceLimit(MyPublic::class.java, 1)
            anyConstructed<Builder>().setClassInstanceLimit(MyInternal::class.java, 2)
            anyConstructed<Builder>().setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate", 3)
            anyConstructed<Builder>().setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate\$MyInner", 4)
            anyConstructed<Builder>().setClassInstanceLimit("org.bubenheimer.instancelimit.MyPrivate\$MyNested", 5)
            anyConstructed<Builder>().build()
            StrictMode.setVmPolicy(vmPolicy)
        }

        confirmVerified(vmPolicyBuilder, vmPolicy)
    }
}

private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader()

private fun Builder.setClassInstanceLimit(name: String, limit: Int) {
    setClassInstanceLimit(Class.forName(name), limit)
}

@InstanceLimit(1)
public class MyPublic

@InstanceLimit(2)
internal class MyInternal

@InstanceLimit(3)
private class MyPrivate {
    @InstanceLimit(4)
    inner class MyInner

    @InstanceLimit(5)
    class MyNested
}
