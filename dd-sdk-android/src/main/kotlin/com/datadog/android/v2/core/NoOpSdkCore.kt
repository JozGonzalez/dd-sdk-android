/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.v2.core

import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.v2.api.FeatureEventReceiver
import com.datadog.android.v2.api.FeatureScope
import com.datadog.android.v2.api.FeatureStorageConfiguration
import com.datadog.android.v2.api.FeatureUploadConfiguration
import com.datadog.android.v2.api.SdkCore
import com.datadog.android.v2.api.context.TimeInfo
import com.datadog.android.v2.api.context.UserInfo
import java.util.concurrent.TimeUnit

internal class NoOpSdkCore : SdkCore {
    override val time: TimeInfo = with(System.currentTimeMillis()) {
        TimeInfo(
            deviceTimeNs = TimeUnit.MILLISECONDS.toNanos(this),
            serverTimeNs = TimeUnit.MILLISECONDS.toNanos(this),
            serverTimeOffsetNs = 0L,
            serverTimeOffsetMs = 0L
        )
    }

    override fun registerFeature(
        featureName: String,
        storageConfiguration: FeatureStorageConfiguration,
        uploadConfiguration: FeatureUploadConfiguration
    ) = Unit

    override fun getFeature(featureName: String): FeatureScope? = null

    override fun setVerbosity(level: Int) = Unit

    override fun getVerbosity(): Int = 0

    override fun setTrackingConsent(consent: TrackingConsent) = Unit

    override fun setUserInfo(userInfo: UserInfo) = Unit

    override fun addUserProperties(extraInfo: Map<String, Any?>) = Unit

    override fun stop() = Unit

    override fun clearAllData() = Unit

    override fun flushStoredData() = Unit

    override fun updateFeatureContext(
        featureName: String,
        updateCallback: Function1<MutableMap<String, Any?>, Unit>
    ) = Unit

    override fun getFeatureContext(featureName: String): Map<String, Any?> = emptyMap()

    override fun setEventReceiver(featureName: String, `receiver`: FeatureEventReceiver) = Unit

    override fun removeEventReceiver(featureName: String) = Unit
}
