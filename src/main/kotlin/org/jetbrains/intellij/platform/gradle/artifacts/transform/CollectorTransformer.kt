// Copyright 2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.artifacts.transform

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ZIP_TYPE
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.kotlin.dsl.registerTransform
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.Configurations.Attributes
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.JETBRAINS_MARKETPLACE_MAVEN_GROUP
import org.jetbrains.intellij.platform.gradle.asPath
import org.jetbrains.intellij.platform.gradle.collectIntelliJPlatformDependencyJars
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

@DisableCachingByDefault(because = "Not worth caching")
abstract class CollectorTransformer : TransformAction<TransformParameters.None> {

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val input = inputArtifact.asPath

        if (input.name.startsWith(JETBRAINS_MARKETPLACE_MAVEN_GROUP)) {
            // Plugin dependency
            input.forEachDirectoryEntry { entry ->
                entry.resolve("lib")
                    .listDirectoryEntries("*.jar")
                    .forEach { outputs.file(it) }
            }
        } else {
            // TODO: check if the given directory is IDEA — i.e. by checking if there's product-info.json file
            // IntelliJ Platform SDK dependency
            collectIntelliJPlatformDependencyJars(input).forEach {
                outputs.file(it)
            }
        }
    }
}

internal fun DependencyHandler.applyCollectorTransformer(
    compileClasspathConfiguration: Configuration,
    testCompileClasspathConfiguration: Configuration,
) {
    // ZIP archives fetched from the IntelliJ Maven repository
    artifactTypes.maybeCreate(ZIP_TYPE)
        .attributes
        .attribute(Attributes.collected, false)

    // Local IDEs pointed with intellijPlatformLocal dependencies helper
    artifactTypes.maybeCreate(DIRECTORY_TYPE)
        .attributes
        .attribute(Attributes.collected, false)

    compileClasspathConfiguration
        .attributes
        .attribute(Attributes.collected, true)

    testCompileClasspathConfiguration
        .attributes
        .attribute(Attributes.collected, true)

    registerTransform(CollectorTransformer::class) {
        from
            .attribute(Attributes.extracted, true)
            .attribute(Attributes.collected, false)
        to
            .attribute(Attributes.extracted, true)
            .attribute(Attributes.collected, true)
    }
}