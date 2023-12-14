// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle

import org.gradle.api.component.SoftwareComponent

class IntelliJPlatformPluginLibrary : SoftwareComponent {

    override fun getName() = "intellij-platform-plugin"
}