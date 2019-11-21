/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-2019 Datadog, Inc.
 */

package com.datadog.gradle.config

import com.datadog.gradle.utils.Version

object AndroidConfig {

    const val TARGET_SDK = 29
    const val MIN_SDK = 16
    const val BUILD_TOOLS_VERSION = "29.0.2"

    val VERSION = Version(0, 0, 1)
}