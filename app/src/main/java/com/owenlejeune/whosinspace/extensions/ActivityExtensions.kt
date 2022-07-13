package com.owenlejeune.whosinspace.extensions

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.layout.WindowMetricsCalculator

@Composable
fun Activity.rememberWindowSize(): Size {
    val configuration = LocalConfiguration.current

    val windowMetrics = remember(configuration) {
        WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
    }
    return windowMetrics.bounds.toComposeRect().size
}

enum class WindowSizeClass { Compact, Medium, Expanded }

@Composable
fun Activity.rememberWindowSizeClass(): WindowSizeClass {
    val windowSize = rememberWindowSize()

    val windowSizeDp = with(LocalDensity.current) {
        windowSize.toDpSize()
    }

    return getWindowSizeClass(windowSizeDp)
}

private fun getWindowSizeClass(windowDpSize: DpSize): WindowSizeClass = when {
    windowDpSize.width < 0.dp -> throw IllegalArgumentException("Dp value cannot be negative")
    windowDpSize.width < 600.dp -> WindowSizeClass.Compact
    windowDpSize.width < 840.dp -> WindowSizeClass.Medium
    else -> WindowSizeClass.Expanded
}

fun Activity.getOrientation() = resources.configuration.orientation