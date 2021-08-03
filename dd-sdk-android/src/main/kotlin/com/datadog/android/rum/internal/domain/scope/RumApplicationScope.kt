/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.rum.internal.domain.scope

import com.datadog.android.core.internal.net.FirstPartyHostDetector
import com.datadog.android.core.internal.persistence.DataWriter
import com.datadog.android.core.internal.time.TimeProvider
import com.datadog.android.rum.internal.domain.RumContext
import com.datadog.android.rum.internal.domain.event.RumEvent
import com.datadog.android.rum.internal.vitals.VitalMonitor

internal class RumApplicationScope(
    applicationId: String,
    internal val samplingRate: Float,
    internal val backgroundTrackingEnabled: Boolean,
    firstPartyHostDetector: FirstPartyHostDetector,
    cpuVitalMonitor: VitalMonitor,
    memoryVitalMonitor: VitalMonitor,
    frameRateVitalMonitor: VitalMonitor,
    timeProvider: TimeProvider
) : RumScope {

    private val rumContext = RumContext(applicationId = applicationId)
    internal val childScope: RumScope = RumSessionScope(
        this,
        samplingRate,
        backgroundTrackingEnabled,
        firstPartyHostDetector,
        cpuVitalMonitor,
        memoryVitalMonitor,
        frameRateVitalMonitor,
        timeProvider
    )

    // region RumScope

    override fun handleEvent(
        event: RumRawEvent,
        writer: DataWriter<RumEvent>
    ): RumScope {
        childScope.handleEvent(event, writer)
        return this
    }

    override fun getRumContext(): RumContext {
        return rumContext
    }

    // endregion
}
