/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal

import com.datadog.android.sessionreplay.internal.SessionReplayFeature
import com.datadog.android.utils.forge.Configurator
import com.datadog.android.v2.api.context.DatadogContext
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class FeaturesContextResolverTest {

    lateinit var testedFeaturesContextResolver: FeaturesContextResolver

    @BeforeEach
    fun `set up`() {
        testedFeaturesContextResolver = FeaturesContextResolver()
    }

    @Test
    fun `M return true W resolveHasReplay {sessionReplayContext isRecording}`(forge: Forge) {
        // Given
        val fakeSdkContext = forge.getForgery<DatadogContext>()
            .copy(
                featuresContext = mapOf(
                    SessionReplayFeature.SESSION_REPLAY_FEATURE_NAME to
                        mapOf(SessionReplayFeature.IS_RECORDING_CONTEXT_KEY to true)
                )

            )

        // When
        val hasReplay = testedFeaturesContextResolver.resolveHasReplay(fakeSdkContext)
        assertThat(hasReplay).isTrue
    }

    @Test
    fun `M return false W resolveHasReplay {sessionReplayContext isNotRecording}`(forge: Forge) {
        // Given
        val fakeSdkContext = forge.getForgery<DatadogContext>()
            .copy(
                featuresContext = mapOf(
                    SessionReplayFeature.SESSION_REPLAY_FEATURE_NAME to
                        mapOf(SessionReplayFeature.IS_RECORDING_CONTEXT_KEY to false)
                )

            )

        // When
        val hasReplay = testedFeaturesContextResolver.resolveHasReplay(fakeSdkContext)
        assertThat(hasReplay).isFalse
    }

    @Test
    fun `M return false W resolveHasReplay {sessionReplayContext does not exist}`(forge: Forge) {
        // Given
        val fakeSdkContext = forge.getForgery<DatadogContext>()
            .copy(featuresContext = emptyMap())

        // When
        val hasReplay = testedFeaturesContextResolver.resolveHasReplay(fakeSdkContext)
        assertThat(hasReplay).isFalse
    }

    @Test
    fun `M return false W resolveHasReplay {isRecording context key does not exist}`(forge: Forge) {
        // Given
        val fakeSdkContext = forge.getForgery<DatadogContext>()
            .copy(
                featuresContext = mapOf(
                    SessionReplayFeature.SESSION_REPLAY_FEATURE_NAME to
                        emptyMap()
                )
            )

        // When
        val hasReplay = testedFeaturesContextResolver.resolveHasReplay(fakeSdkContext)
        assertThat(hasReplay).isFalse
    }
}
