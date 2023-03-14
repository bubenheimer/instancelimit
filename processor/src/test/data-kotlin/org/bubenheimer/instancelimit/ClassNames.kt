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

@InstanceLimit(4)
object `Ding` {
    @InstanceLimit(5)
    class Yo
}

@InstanceLimit(1)
object Ding2 {
    @InstanceLimit(0)
    private class Yo
}

@InstanceLimit(2)
class Me {
    @InstanceLimit(3)
    class You
}

@InstanceLimit(1)
class `Ni$e`

@InstanceLimit(1)
class `Ni"e`

@InstanceLimit(6)
private class Mine {
    @InstanceLimit(7)
    private class MineToo(private val mine: Int) {
        @InstanceLimit(8)
        private inner class AlsoMine {
            fun yes() {
//                @InstanceLimit(9)
                class LocalMine
            }
        }

        private var myVal: String = "7"
            get() {
//                @InstanceLimit(10)
                class HiFive

                return field
            }
            set(value) {
//                @InstanceLimit(11)
                class HiSeven

                field = value
            }

        private var myVal2: String
            get() {
//                @InstanceLimit(10)
                class HiFive

                return "Hi"
            }
            set(value) {
//                @InstanceLimit(11)
                class HiSeven

                myVal = value
            }

        private fun myFun() {
//            @InstanceLimit(1)
            class HiSix
        }
    }
}

@InstanceLimit(1)
private class MoreMine {
    @InstanceLimit(1)
    class MineToo(private val mine: Int) {
        @InstanceLimit(1)
        private inner class AlsoMine {
            fun yes() {
//                @InstanceLimit(1)
                class LocalMine
            }
        }

        fun myFun() {
//            @InstanceLimit(1)
            class FunClass
        }
    }

    @JvmName("nothisone")
    fun namingFun() {
//        @InstanceLimit(3)
        class WhatsThis
    }

    @get:JvmName("otherprop")
    var myProp: Int
        get() = 1
        set(value) {
//            @InstanceLimit(5)
            class Yippee
        }
}

@InstanceLimit(1)
private class FinalMine

abstract class Hello

@InstanceLimit(1)
class Hello2 : Hello() {
    internal fun mangled() {
//        @InstanceLimit(1)
        class WhatsMyName
    }

    internal val alsoMangled: String
        get() {
//            @InstanceLimit(1)
            class WhatsTheBigDeal
            return "hi"
        }
}

fun doit() {
//    @InstanceLimit(1)
    open class LocalMine {
//        @InstanceLimit(1)
        inner class InMine
    }

//    @InstanceLimit(1)
    object : LocalMine() {
//        @InstanceLimit(1)
        inner class Too
    }
}

@InstanceLimit(2)
internal class ModuleOnly {
    @InstanceLimit(2)
    class Contained
}

@InstanceLimit(2)
class Everywhere {
    @InstanceLimit(2)
    internal class Contained

    @InstanceLimit(2)
    private class OnlyHere {
        @InstanceLimit(2)
        internal class Contained2
    }
}
