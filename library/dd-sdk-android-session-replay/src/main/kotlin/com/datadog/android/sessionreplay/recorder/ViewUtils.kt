/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package com.datadog.android.sessionreplay.recorder

import android.os.Build
import android.view.View
import android.view.ViewStub
import androidx.appcompat.widget.ActionBarContextView
import androidx.appcompat.widget.Toolbar

internal class ViewUtils {

    private val systemViewIds by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOf(android.R.id.navigationBarBackground, android.R.id.statusBarBackground)
        } else {
            emptySet()
        }
    }

    internal fun checkIfNotVisible(view: View): Boolean {
        return !view.isShown || view.width <= 0 || view.height <= 0
    }

    internal fun checkIfSystemNoise(view: View): Boolean {
        return view.id in systemViewIds ||
            view is ViewStub ||
            view is ActionBarContextView
    }

    internal fun checkIsToolbar(view: View): Boolean {
        return (
            Toolbar::class.java.isAssignableFrom(view::class.java) &&
                view.id == androidx.appcompat.R.id.action_bar
            ) ||
            (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    android.widget.Toolbar::class.java
                        .isAssignableFrom(view::class.java)
                )
    }
}
