# InstanceLimit
#### Annotations for Android StrictMode setClassInstanceLimit()

A KSP-based processor providing a declarative approach based on an
[@InstanceLimit annotation](api/src/main/kotlin/org/bubenheimer/instancelimit/InstanceLimit.kt)
for tracking Java and Kotlin class instances. This is primarily intended for Android, via
[StrictMode.VmPolicy.Builder.setClassInstanceLimit()](https://developer.android.com/reference/android/os/StrictMode.VmPolicy.Builder#setClassInstanceLimit(java.lang.Class,%20int))

Multi-module builds are supported, and even external libraries built separately, both Android libraries
and plain Java libraries.

The processor is compatible with Kotlin multiplatform builds as well; the processor output could
be used on non-Android Java platforms. Currently, Android is the only platform with a
[ready-made API](api-android/src/main/kotlin/org/bubenheimer/instancelimit/InstanceLimits.kt). 

## Usage

```kotlin
import org.bubenheimer.instancelimit.InstanceLimit

// Singleton
@InstanceLimit(1)
class GlobalStuff {
    @InstanceLimit(2)
    inner data class OtherStuff(val value: String)

    // Generates code for access via reflection
    @InstanceLimit(3)
    private class JustACouple
}

// Validate that no actual inline class instances are created
@InstanceLimit(0)
@JvmInline
value class Wrapper(value: Int)
```

Running the annotation processor on this snippet generates lists of instance counts, which can then
be applied, commonly at app startup time:

```kotlin
import org.bubenheimer.instancelimit.setInstanceLimits

class MyApp : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        // Set instance limits for public and non-public classes;
        // classLoader is properly initialized only after super.onCreate()
        setInstanceLimits(classLoader)
    }
}
```

Only a single call to `setInstanceLimits()` is needed for the entire app; this will set instance limits
for all modules and external libraries at once.

Dig into the (small)
[public API](api-android/src/main/kotlin/org/bubenheimer/instancelimit/InstanceLimits.kt)
to discover alternative initialization options.

## Build configuration

Modules that use `@InstanceLimit` use this Gradle build setup (Groovy version); these artifacts
are not Android-specific.

```groovy
plugins {
    id('com.google.devtools.ksp')
}

dependencies {
    // Provide @InstanceLimit annotation and base class for generated code
    implementation("org.bubenheimer.instancelimit:api:<version>")
    // Run KSP processor on module and generate code
    ksp("org.bubenheimer.instancelimit:processor:<version>")
}
```

KSP plugin usage looks a little different in multiplatform scenarios:
<https://kotlinlang.org/docs/ksp-multiplatform.html> 

In addition, only for the module containing the instance limits initialization code above:
```groovy
dependencies {
    implementation("org.bubenheimer.instancelimit:api-android:<version>")
}
```

## Configuration options

The processor typically logs problems as warnings, not errors, because problems with
instance limits do not really affect the correctness of the build. To turn warnings into
errors use this KSP configuration block:

```groovy
ksp {
    arg("instanceLimits.skipAnalyzerErrors", "false")
}
```

## Tips

Please do not run the processor on your final release build and do not call `setInstanceLimits()` there!

## Limitations

The annotation processor currently can only process non-local classes; classes that are
declared inside a function or inside a property getter or setter are not processed. There
is no compilation error or warning for local annotated classes. This limitation is due to
several issues and limitations in KSP and in the Kotlin compiler. You are welcome to vote
on these issues and participate in the discussion, to hopefully see them resolved at a later time:

1. KSP crash when getting the full name of local classes:
   https://github.com/google/ksp/issues/1335
2. ~~Local classes in property getters and setters are unsupported in KSP:
   https://github.com/google/ksp/issues/1332~~
3. The processor needs access to binary class names:
   https://github.com/google/ksp/issues/1336
4. KSP ignores local Java classes, presumably mimicking Java annotation processing: 
   https://github.com/google/ksp/issues/1345

## Implementation details

For each module on which it is run with some meaningful output, the processor generates a single
class extending
[InstanceLimitsProvider](api/src/main/kotlin/org/bubenheimer/instancelimit/InstanceLimitsProvider.kt).
Generated classes are surfaced via the
[Java ServiceLoader pattern](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/ServiceLoader.html).

## License

    Copyright 2023 Uli Bubenheimer

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
