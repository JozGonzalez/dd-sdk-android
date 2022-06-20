/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.utils.forge

import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

internal class UserAdditionalPropertiesForgeryFactory : ForgeryFactory<Map<String, Any?>> {

    override fun getForgery(forge: Forge): Map<String, Any?> {
        return forge.exhaustiveAttributes()
    }
}
