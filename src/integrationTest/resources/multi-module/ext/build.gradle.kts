// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

val intellijPlatformTypeProperty = providers.gradleProperty("intellijPlatform.type")
val intellijPlatformVersionProperty = providers.gradleProperty("intellijPlatform.version")

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(intellijPlatformTypeProperty, intellijPlatformVersionProperty)
        bundledPlugins("com.intellij.java")
        instrumentationTools()

        localPlugin(project(":base"))
        pluginModule(implementation(project(":submodule")))
    }

    implementation(project(":raw"))
    implementation("org.jetbrains:markdown:0.7.3")
}

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false
}
