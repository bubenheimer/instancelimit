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
        private inner class AlsoMine
    }
}

@InstanceLimit(1)
private class MoreMine {
    @InstanceLimit(1)
    class MineToo(private val mine: Int) {
        @InstanceLimit(1)
        private inner class AlsoMine
    }
}

@InstanceLimit(1)
private class FinalMine

abstract class Hello

@InstanceLimit(1)
class Hello2 : Hello()

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
