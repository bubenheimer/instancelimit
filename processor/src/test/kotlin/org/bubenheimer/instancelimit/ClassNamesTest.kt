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

import java.util.ServiceLoader
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.fail

class ClassNamesTest {
    @Test
    fun loadReflective() {
        serviceLoader.forEach {
            it.reflective.forEach { (className, _) ->
                Class.forName(className, false, javaClass.classLoader)
            }
        }
    }

    @Test
    fun compareReflective() {
        val reflectiveGenerated: Set<Pair<String, Int>> = serviceLoader.single().reflective.toSet()

        val generatedExtra: Set<Pair<String, Int>> = reflectiveGenerated - reflectiveExpected

        if (generatedExtra.isNotEmpty()) {
            fail("Extra reflective generated elements:\n${prettyPrint(generatedExtra)}")
        }

        val expectedExtra: Set<Pair<String, Int>> = reflectiveExpected - reflectiveGenerated

        if (expectedExtra.isNotEmpty()) {
            fail("Extra reflective expected elements:\n${prettyPrint(expectedExtra)}")
        }
    }

    @Test
    fun compareNonReflective() {
        val nonReflectiveGenerated: Set<Pair<KClass<*>, Int>> =
            serviceLoader.single().nonReflective.toSet()

        val generatedExtra: Set<Pair<KClass<*>, Int>> =
            nonReflectiveGenerated - nonReflectiveExpected

        if (generatedExtra.isNotEmpty()) {
            fail("Extra non-reflective generated elements:\n${prettyPrint(generatedExtra)}")
        }

        val expectedExtra: Set<Pair<KClass<*>, Int>> = nonReflectiveExpected - nonReflectiveGenerated

        if (expectedExtra.isNotEmpty()) {
            fail("Extra non-reflective expected elements:\n${prettyPrint(expectedExtra)}")
        }
    }

    private fun prettyPrint(data: Iterable<*>): String = buildString {
        data.forEach {
            append(it)
            appendLine()
        }
    }
}

private val serviceLoader: ServiceLoader<InstanceLimitsProvider> =
    ServiceLoader.load(InstanceLimitsProvider::class.java)

private val nonReflectiveExpected: Set<Pair<KClass<*>, Int>> = setOf(
    org.bubenheimer.instancelimit.AndAlso::class to 1,
    org.bubenheimer.instancelimit.`Ano$er`::class to 4,
    org.bubenheimer.instancelimit.WithFun::class to 12,
    org.bubenheimer.instancelimit.`$pecial`.Normal::class to 1,

    org.bubenheimer.instancelimit.Ding::class to 4,
    org.bubenheimer.instancelimit.Ding.Yo::class to 5,
    org.bubenheimer.instancelimit.Ding2::class to 1,
    org.bubenheimer.instancelimit.Me::class to 2,
    org.bubenheimer.instancelimit.Me.You::class to 3,
    org.bubenheimer.instancelimit.`Ni$e`::class to 1,
    org.bubenheimer.instancelimit.`Ni"e`::class to 1,
    org.bubenheimer.instancelimit.Hello2::class to 1,
    org.bubenheimer.instancelimit.ModuleOnly::class to 2,
    org.bubenheimer.instancelimit.ModuleOnly.Contained::class to 2,
    org.bubenheimer.instancelimit.Everywhere::class to 2,
    org.bubenheimer.instancelimit.Everywhere.Contained::class to 2,
)

private val reflectiveExpected: Set<Pair<String, Int>> = setOf(
    "MyJavaTypeInDefault" to 2,
    "MySecondJavaTypeInDefault" to 3,
    "MyOtherJavaTypeInDefault" to 2,
    "org.bubenheimer.instancelimit.AndAlso\$Inside" to 11,
    "org.bubenheimer.instancelimit.Ano\$er\$Within" to 5,
    "org.bubenheimer.instancelimit.Elsewhere" to 6,
    "org.bubenheimer.instancelimit.Elsewhere\$Inside" to 7,
    "org.bubenheimer.instancelimit.Elsewhere\$Inside2" to 8,
//    "org.bubenheimer.instancelimit.WithFun\$today\$Rain" to 13,
//    "org.bubenheimer.instancelimit.WithFun\$tomorrow\$Snow" to 15,
//    "org.bubenheimer.instancelimit.WithFun\$tomorrow\$Snow" to 14,

    "MyTypeInDefault" to 2,
    "org.bubenheimer.instancelimit.Ding2\$Yo" to 0,
    "org.bubenheimer.instancelimit.Mine" to 6,
    "org.bubenheimer.instancelimit.Mine\$MineToo" to 7,
    "org.bubenheimer.instancelimit.Mine\$MineToo\$AlsoMine" to 8,
//    "org.bubenheimer.instancelimit.Mine\$MineToo\$AlsoMine\$yes\$LocalMine" to 9,
//    "org.bubenheimer.instancelimit.Mine\$MineToo\$myVal\$HiFive" to 10,
//    "org.bubenheimer.instancelimit.Mine\$MineToo\$myVal\$HiSeven" to 11,
//    "org.bubenheimer.instancelimit.Mine\$MineToo\$myFun\$HiSix" to 1,
    "org.bubenheimer.instancelimit.MoreMine" to 1,
    "org.bubenheimer.instancelimit.MoreMine\$MineToo" to 1,
//    "org.bubenheimer.instancelimit.MoreMine\$MineToo\$myFun\$FunClass" to 1,
    "org.bubenheimer.instancelimit.MoreMine\$MineToo\$AlsoMine" to 1,
//    "org.bubenheimer.instancelimit.MoreMine\$MineToo\$AlsoMine\$yes\$LocalMine" to 1,
//    "org.bubenheimer.instancelimit.MoreMine\$nothisone\$WhatsThis" to 1,
    "org.bubenheimer.instancelimit.FinalMine" to 1,
//    "org.bubenheimer.instancelimit.ClassNamesKt\$doit\$LocalMine\$InMine" to 1,
//    "org.bubenheimer.instancelimit.ClassNamesKt\$doit\$LocalMine\$Too" to 1,
    "org.bubenheimer.instancelimit.Everywhere\$OnlyHere" to 2,
    "org.bubenheimer.instancelimit.Everywhere\$OnlyHere\$Contained2" to 2,
//    "org.bubenheimer.instancelimit.NotSoSimple\$simple\$VerySimple" to 4,
//    "org.bubenheimer.instancelimit.NotSoSimple\$simpleProp\$PropSimple" to 10,
//    "org.bubenheimer.instancelimit.SomeThingElseKt\$myfun\$MyClass" to 9,
//    "org.bubenheimer.instancelimit.ǳomeThingKt\$myfun2\$MyClass" to 9,
)
