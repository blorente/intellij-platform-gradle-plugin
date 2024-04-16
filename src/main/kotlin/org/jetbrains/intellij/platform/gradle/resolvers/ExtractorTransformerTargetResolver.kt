// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.resolvers

import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.jetbrains.intellij.platform.gradle.Constants.JETBRAINS_MARKETPLACE_MAVEN_GROUP
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.artifacts.transform.ExtractorTransformer
import org.jetbrains.intellij.platform.gradle.models.Coordinates
import org.jetbrains.intellij.platform.gradle.utils.Logger
import java.io.File.separator
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * Resolves the target directory name for the [ExtractorTransformer] dependency transformer.
 * Based on the [artifactPath], it decides about the type of the handled dependency and produces the output directory name.
 * For unknown dependencies, throw an exception.
 *
 * @param artifactPath The path of the artifact to handle.
 */
class ExtractorTransformerTargetResolver(
    private val artifactPath: Path,
    private val intellijPlatformDependency: FileCollection,
    private val intellijPlatformPluginDependency: FileCollection,
    private val jetbrainsRuntimeDependency: FileCollection,
) : Resolver<String> {

    override val subject = "Extractor Transformer Target"

    override val subjectInput = artifactPath

    override val log = Logger(javaClass)

    private val coordinates
        /**
         * @throws GradleException
         */
        @Throws(GradleException::class)
        get() = runCatching {
            val (groupId, artifactId, version) = artifactPath.pathString.split(separator).dropLast(2).takeLast(3)
            Triple(groupId, artifactId, version)
        }.onFailure {
            throw GradleException("Unknown structure of the artifact path: $artifactPath", it)
        }.getOrThrow()

    private val groupId
        get() = coordinates.first

    private val artifactId
        get() = coordinates.second

    private val version
        get() = coordinates.third

    /**
     * @throws GradleException
     */
    @Throws(GradleException::class)
    override fun resolve() = when (obtainArtifactType()) {
        ArtifactType.INTELLIJ_PLATFORM -> {
            val coordinates = Coordinates(groupId, artifactId)
            IntelliJPlatformType.values()
                .firstOrNull { it.dependency == coordinates }
                ?.let { "$it-$version" }
        }

        ArtifactType.INTELLIJ_PLATFORM_PLUGIN -> {
            val channel = when {
                groupId == JETBRAINS_MARKETPLACE_MAVEN_GROUP -> ""
                groupId.endsWith(".$JETBRAINS_MARKETPLACE_MAVEN_GROUP") -> groupId.dropLast(JETBRAINS_MARKETPLACE_MAVEN_GROUP.length + 1)
                else -> null
            }

            channel?.let {
                "$groupId-$artifactId-$version" + "@$channel".takeUnless { channel.isEmpty() }.orEmpty()
            }
        }

        ArtifactType.JETBRAINS_RUNTIME -> {
            version
                .takeIf { groupId == "com.jetbrains" && artifactId == "jbr" }
        }

        else -> null
    } ?: throw GradleException("Cannot resolve '$subject' for: $artifactPath")

    private fun obtainArtifactType() = when (artifactPath.toFile()) {
        in intellijPlatformDependency -> ArtifactType.INTELLIJ_PLATFORM
        in intellijPlatformPluginDependency -> ArtifactType.INTELLIJ_PLATFORM_PLUGIN
        in jetbrainsRuntimeDependency -> ArtifactType.JETBRAINS_RUNTIME
        else -> null
    }

    enum class ArtifactType {
        INTELLIJ_PLATFORM, INTELLIJ_PLATFORM_PLUGIN, JETBRAINS_RUNTIME,
    }
}
