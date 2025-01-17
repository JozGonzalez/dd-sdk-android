/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.v2.core.internal.storage

import com.datadog.android.v2.api.context.DatadogContext

internal interface ContextAwareMapper<R, T> {

    fun map(datadogContext: DatadogContext, model: R): T
}
