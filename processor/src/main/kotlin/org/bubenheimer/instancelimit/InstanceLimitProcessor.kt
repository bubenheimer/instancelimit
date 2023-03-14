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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.ClassKind.CLASS
import com.google.devtools.ksp.symbol.ClassKind.OBJECT
import com.google.devtools.ksp.symbol.Modifier.ABSTRACT
import com.google.devtools.ksp.symbol.Modifier.ANNOTATION
import com.google.devtools.ksp.symbol.Modifier.INTERNAL
import com.google.devtools.ksp.symbol.Modifier.PRIVATE
import com.google.devtools.ksp.symbol.Modifier.PROTECTED
import com.google.devtools.ksp.symbol.Modifier.PUBLIC
import com.google.devtools.ksp.symbol.Modifier.SEALED
import com.google.devtools.ksp.symbol.Origin.JAVA
import com.google.devtools.ksp.symbol.Origin.JAVA_LIB
import java.lang.Character.MAX_RADIX
import java.util.UUID
import kotlin.reflect.KClass

internal class InstanceLimitProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger: KSPLogger
        get() = environment.logger

    private val specs: MutableList<InstanceLimitSpec> = mutableListOf()

    //TODO local classes disabled due to ksp crashes: https://github.com/google/ksp/issues/1335
//    private val visitLocalClasses: Boolean =
//        environment.options["instanceLimits.visitLocalClasses"]?.toBoolean() == true

    private val visitLocalClasses: Boolean = false

    private val skipAnalyzerErrors: Boolean =
        environment.options["instanceLimits.skipAnalyzerErrors"]?.toBoolean() != false

    private fun addInstanceLimit(
        classDeclaration: KSClassDeclaration,
        limit: Int
    ) {
        specs.add(
            InstanceLimitSpec(
                createClassInfo(classDeclaration),
                limit,
                source = classDeclaration.containingFile!!
            )
        )
    }

    private fun createClassInfo(classDeclaration: KSClassDeclaration): ClassInfo {
        val packageName: String = classDeclaration.packageName.asString()

        // Special case for classes in default package (rare): use reflection.
        // Alternatively could use an import statement, but complicates codegen;
        // putting generated code in default package instead might cause other problems.
        val useReflection: Boolean = packageName.isBlank()
                || !classDeclaration.moduleVisibility()

        return if (useReflection) {
            val path: List<String> = classDeclaration.buildPath()

            val pathString: String = path.joinToString("\$")

            //TODO below approaches can crash KSP on local classes:
            // https://github.com/google/ksp/issues/1335

            //TODO only available in ksp PR for now:
            // https://github.com/google/ksp/issues/1336
//            @OptIn(KspExperimental::class)
//            val pathString: String = resolver.mapToJvmClassName(classDeclaration)!!

            val literal: String = pathString.stringLiteral()

            ClassInfo.JVMBinaryName(literal)
        } else {
            val qualifiedName: String = classDeclaration.qualifiedName!!.asString()
            val cleansedName = qualifiedName.splitToSequence('.')
                .map { if (it.contains(qualifiedNameSpecialChars)) "`$it`" else it }
                .joinToString(separator = ".")
            ClassInfo.QualifiedName(cleansedName)
        }
    }

    private fun String.stringLiteral(): String = buildString {
        append('"')

        this@stringLiteral.forEach {
            when (it) {
                '\\' -> append("\\\\")
                '$' -> append("\\$")
                '"' -> append("\\\"")
                else -> append(it)
            }
        }

        append('"')
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        //TODO Ignores annotations inside property getters & setters, due to KSP limitation:
        // https://github.com/google/ksp/issues/1332
        resolver.getSymbolsWithAnnotation(instanceLimitQualifiedName, inDepth = visitLocalClasses)
            .forEach {
                try {
                    process(it)
                } catch (t: Throwable) {
                    // Catch unexpected errors during processing and log with offending element

                    // print unspecified errors with code reference
                    logAnalyzerError("$t (stacktrace available at INFO level)", it)

                    logger.info(t.stackTraceToString(), it)
                }
            }

        return emptyList()
    }

    private fun process(symbol: KSAnnotated) {
        if (symbol !is KSClassDeclaration) {
            logAnalyzerError("@$instanceLimitClassName only valid on classes", symbol)

            return
        }

        when (symbol.classKind) {
            CLASS -> if (symbol.modifiers.any {
                    when (it) {
                        ABSTRACT, SEALED, ANNOTATION -> true
                        else -> false
                    }
            }) {
                logAnalyzerError(
                    "@$instanceLimitClassName not available on" +
                            " abstract, sealed, and annotation classes",
                    symbol
                )

                return
            }

            OBJECT -> {}

            else -> {
                logAnalyzerError(
                    "@$instanceLimitClassName only supports concrete classes and objects",
                    symbol
                )

                return
            }
        }

        @OptIn(KspExperimental::class)
        val annotation: InstanceLimit? =
            symbol.getAnnotationsByType(instanceLimitKClass).singleOrNull()

        if (annotation == null) {
            logAnalyzerError(
                "Invalid @$instanceLimitClassName annotation usage. " +
                        "Use a non-repeating annotation with a non-negative Int argument.",
                symbol
            )

            return
        }

        val instanceLimit: Int = try {
            annotation.value
        } catch (e: ClassCastException) {
            logAnalyzerError("@$instanceLimitClassName argument must be Int", symbol)

            return
        } catch (e: NoSuchElementException) {
            logAnalyzerError("@$instanceLimitClassName takes a single Int argument", symbol)

            return
        }

        if (instanceLimit < 0) {
            logAnalyzerError("@$instanceLimitClassName value must be non-negative", symbol)

            return
        }

        addInstanceLimit(symbol, instanceLimit)
    }

    override fun finish() {
        if (specs.isEmpty()) return

        // Remove duplicates for faster downstream processing
        val sources: Set<KSFile> = LinkedHashSet(specs.map { it.source })

        val dependencies = Dependencies(
            aggregating = true,
            sources = sources.toTypedArray()
        )

        val codeGenerator: CodeGenerator = environment.codeGenerator

        val instanceLimitsProviderClassName: String = newInstanceLimitsProviderClassName()

        codeGenerator.createNewFile(
            dependencies,
            INSTANCE_LIMITS_PROVIDER_CLASS_PACKAGE,
            instanceLimitsProviderClassName
        ).buffered().use {
            it.write(
                generateInstanceLimitsProvider(instanceLimitsProviderClassName, specs).toByteArray()
            )
        }

        codeGenerator.createNewFile(
            dependencies,
            packageName = "",
            fileName = resourceFileName,
            extensionName = ""
        ).buffered().use { it.write(resourceFileContents(instanceLimitsProviderClassName)) }
    }

    private tailrec fun KSDeclaration.moduleVisibility(): Boolean {
        return if (this !is KSClassDeclaration) {
            when (this) {
                is KSFunctionDeclaration, is KSPropertyDeclaration -> false
                else -> {
                    logAnalyzerError("Unexpected element $simpleName", this)
                    false
                }
            }
        } else {
            val visibleLocal: Boolean = when {
                modifiers.contains(PRIVATE) -> false
                modifiers.contains(PROTECTED) -> false
                modifiers.contains(PUBLIC) -> true
                modifiers.contains(INTERNAL) -> true
                origin == JAVA -> false // package visibility
                origin == JAVA_LIB -> false // package visibility
                else -> true
            }

            if (!visibleLocal) false else {
                val parentDeclaration: KSDeclaration = parentDeclaration ?: return true
                parentDeclaration.moduleVisibility()
            }
        }
    }

    private fun KSClassDeclaration.buildPath(): List<String> {
        val reversePath: MutableList<String> = mutableListOf()

        buildPath(reversePath)

        return reversePath.asReversed()
    }

    private tailrec fun KSClassDeclaration.buildPath(reversePath: MutableList<String>) {
        val parentDeclaration: KSClassDeclaration? = parentDeclaration as KSClassDeclaration?

        if (parentDeclaration == null) {
            reversePath.add(qualifiedName!!.asString())
        } else {
            reversePath.add(simpleName.asString())

            parentDeclaration.buildPath(reversePath)
        }
    }

    // Old approach, including overly simplistic provisions for local classes
//    private tailrec fun KSDeclaration.assembleBinaryClassName(reversePath: MutableList<String>) {
//        val parentDeclaration: KSDeclaration? = parentDeclaration
//
//        if (parentDeclaration == null) {
//            when (this) {
//                is KSClassDeclaration -> {}
//
//                is KSFunctionDeclaration, is KSPropertyDeclaration -> {
//                    val fileName = containingFile!!.fileName
//
//                    val extension: String = fileName.substring(fileName.length - 3)
//
//                    if (extension.lowercase() == ".kt") {
//                        reversePath.add(
//                            fileName.dropLast(3).replaceFirstChar {
//                                if (it.isLowerCase()) it.uppercaseChar() else it
//                            } + "Kt")
//                    } else {
//                        logAnalyzerError("Unexpected file name extension $extension", this)
//                    }
//                }
//
//                else -> {
//                    logAnalyzerError("Unexpected element $simpleName", this)
//                }
//            }
//        } else {
//            reversePath.add(parentDeclaration.simpleName.asString())
//
//            parentDeclaration.assembleBinaryClassName(reversePath)
//        }
//    }

    /**
     *  Print with code reference
     */
    private fun logAnalyzerError(message: String, symbol: KSNode?) =
        if (skipAnalyzerErrors) logger.warn(message, symbol) else logger.error(message, symbol)
}

internal class InstanceLimitProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment
    ): SymbolProcessor = InstanceLimitProcessor(environment)
}

private sealed interface ClassInfo {
    data class QualifiedName(val path: String) : ClassInfo

    data class JVMBinaryName(val pathLiteral: String) : ClassInfo
}

private val instanceLimitKClass: KClass<InstanceLimit> = InstanceLimit::class

private val instanceLimitClass: Class<InstanceLimit> = instanceLimitKClass.java

private val instanceLimitClassName: String = instanceLimitClass.simpleName

private val instanceLimitQualifiedName: String = instanceLimitClass.canonicalName

private val instanceLimitsProviderKClass = InstanceLimitsProvider::class

private val instanceLimitsProviderClass = instanceLimitsProviderKClass.java

private const val INSTANCE_LIMITS_PROVIDER_CLASS_PACKAGE = "org.bubenheimer.instancelimit"

private const val INSTANCE_LIMITS_PROVIDER_CLASS_BASE_NAME = "InstanceLimitsProvider"

private val resourceFileName = "META-INF/services/${instanceLimitsProviderClass.canonicalName}"

private fun newInstanceLimitsProviderClassName(): String {
    val uuid: UUID = UUID.randomUUID()
    return INSTANCE_LIMITS_PROVIDER_CLASS_BASE_NAME +
            uuid.leastSignificantBits.alphanumeric() +
            uuid.mostSignificantBits.alphanumeric()
}

/**
 * Poor man's alphanumeric encoding for valid class & directory names
 */
private fun Long.alphanumeric() = toULong().toString(MAX_RADIX)

private val qualifiedNameSpecialChars: Regex = Regex("[$\"]")

private fun resourceFileContents(providerClassName: String): ByteArray =
    "$INSTANCE_LIMITS_PROVIDER_CLASS_PACKAGE.$providerClassName".toByteArray()

private data class InstanceLimitSpec(
    val classInfo: ClassInfo,
    val limit: Int,
    val source: KSFile
)

private fun generateInstanceLimitsProvider(
    className: String,
    instanceLimitSpecs: List<InstanceLimitSpec>
): String = buildString {
    val (nonReflectiveSpecs, reflectiveSpecs) =
        instanceLimitSpecs.partition { it.classInfo is ClassInfo.QualifiedName }

    append("""
package $INSTANCE_LIMITS_PROVIDER_CLASS_PACKAGE

public class $className : ${instanceLimitsProviderClass.simpleName}() {

""".trimMargin()
    )

    nonReflectiveSpecs.takeUnless { it.isEmpty() }?.let {
        append("""
    override val nonReflective: List<Pair<kotlin.reflect.KClass<*>, Int>> = listOf(

""".trimMargin())

        it.forEach {
            append("""
        ${(it.classInfo as ClassInfo.QualifiedName).path}::class to ${it.limit},

""".trimMargin())
        }

        append("""
    )

""".trimMargin())
    }

    reflectiveSpecs.takeUnless { it.isEmpty() }?.let {
        append("""
    override val reflective: List<Pair<String, Int>> = listOf(

""".trimMargin())

        it.forEach {
            append("""
        ${(it.classInfo as ClassInfo.JVMBinaryName).pathLiteral} to ${it.limit},

""".trimMargin())
        }

        append("""
    )

""".trimMargin())
    }

    append("""
}

""".trimMargin())
}
