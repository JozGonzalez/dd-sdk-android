/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.webview.internal.log

import com.datadog.android.core.internal.CoreFeature
import com.datadog.android.core.internal.persistence.DataWriter
import com.datadog.android.core.internal.time.TimeProvider
import com.datadog.android.core.internal.utils.sdkLogger
import com.datadog.android.log.LogAttributes
import com.datadog.android.log.model.WebViewLogEvent
import com.datadog.android.webview.internal.rum.WebViewRumEventContextProvider
import com.google.gson.JsonObject
import com.google.gson.JsonParseException

internal class WebViewLogEventConsumer(
    private val userLogsWriter: DataWriter<WebViewLogEvent>,
    private val internalLogsWriter: DataWriter<WebViewLogEvent>,
    private val rumContextProvider: WebViewRumEventContextProvider,
    private val timeProvider: TimeProvider
) {

    private val ddTags: String by lazy {
        "${LogAttributes.APPLICATION_VERSION}:${CoreFeature.packageVersion}" +
            ",${LogAttributes.ENV}:${CoreFeature.envName}"
    }

    fun consume(event: JsonObject, eventType: String) {
        map(event)?.let {
            if (eventType == INTERNAL_LOG_EVENT_TYPE) {
                internalLogsWriter.write(it)
            } else {
                userLogsWriter.write(it)
            }
        }
    }

    private fun map(event: JsonObject): WebViewLogEvent? {
        try {
            addDdTags(event)
            val logEvent = WebViewLogEvent.fromJson(event.toString())
            val rumContext = rumContextProvider.getRumContext()
            val correctedDate = logEvent.date + timeProvider.getServerOffsetMillis()
            return if (rumContext != null) {
                val resolvedProperties = logEvent.additionalProperties.toMutableMap()
                resolvedProperties[LogAttributes.RUM_APPLICATION_ID] = rumContext.applicationId
                resolvedProperties[LogAttributes.RUM_SESSION_ID] = rumContext.sessionId
                logEvent.copy(additionalProperties = resolvedProperties, date = correctedDate)
            } else {
                logEvent.copy(date = correctedDate)
            }
        } catch (e: JsonParseException) {
            sdkLogger.e(JSON_PARSING_ERROR_MESSAGE, e)
            return null
        }
    }

    private fun addDdTags(event: JsonObject) {
        val eventDdTags = event.get(DDTAGS_KEY_NAME)?.asString
        if (eventDdTags.isNullOrEmpty()) {
            event.addProperty(DDTAGS_KEY_NAME, ddTags)
        } else {
            event.addProperty(DDTAGS_KEY_NAME, ddTags + DDTAGS_SEPARATOR + eventDdTags)
        }
    }

    companion object {
        const val DDTAGS_SEPARATOR = ","
        const val DDTAGS_KEY_NAME = "ddtags"
        const val USER_LOG_EVENT_TYPE = "log"
        const val INTERNAL_LOG_EVENT_TYPE = "internal_log"
        const val JSON_PARSING_ERROR_MESSAGE = "The bundled web log event could not be deserialized"
        val LOG_EVENT_TYPES = setOf(USER_LOG_EVENT_TYPE, INTERNAL_LOG_EVENT_TYPE)
    }
}