/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.core.internal.utils

import com.datadog.android.log.internal.logger.LogHandler
import com.datadog.android.log.internal.logger.NoOpLogHandler
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class RuntimeUtilsReleaseTest {

    // region sdkLogger

    @Test
    fun `M build noop sdkLogger W buildSdkLogger() {InternalLogs off}`() {
        // When
        val logger = buildSdkLogger()

        // Then
        val handler: LogHandler = logger.handler
        assertThat(handler).isInstanceOf(NoOpLogHandler::class.java)
    }

    // endregion

}
