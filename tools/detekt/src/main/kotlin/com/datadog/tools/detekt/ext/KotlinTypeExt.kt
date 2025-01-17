/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.tools.detekt.ext

import org.jetbrains.kotlin.descriptors.buildPossiblyInnerType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.supertypes

internal fun KotlinType.fullType(): String? {
    val descriptor = if (toString() == "T" || toString() == "C") {
        // Treat generic types as their closest supertype (usually Any)
        supertypes().firstOrNull()?.buildPossiblyInnerType()?.classifierDescriptor
    } else {
        buildPossiblyInnerType()?.classifierDescriptor
    }
    val fqName = descriptor?.fqNameOrNull()
    return if (fqName != null) {
        val nullableSuffix = if (isNullable()) "?" else ""
        "$fqName$nullableSuffix"
    } else {
        val supertypes = this.supertypes().joinToString()
        println("Unable to get fqName for ${this.javaClass} $this: $supertypes")
        null
    }
}
