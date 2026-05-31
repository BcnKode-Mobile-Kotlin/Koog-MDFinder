package com.juzabel.mdfinder.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter

@Composable
fun CustomTitleBar(
    title: String,
    window: Window?,
    windowState: WindowState,
    onCloseRequest: () -> Unit
) {
    // AWT mouse listeners handle dragging — no Compose gesture interception,
    // so buttons and other composables receive clicks normally.
    DisposableEffect(window) {
        if (window == null) return@DisposableEffect onDispose {}

        val titleBarHeightPx = 40
        var startScreenX = 0
        var startScreenY = 0
        var startWindowX = 0
        var startWindowY = 0
        var dragging = false

        val mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.y <= titleBarHeightPx) {
                    startScreenX = e.xOnScreen
                    startScreenY = e.yOnScreen
                    startWindowX = window.x
                    startWindowY = window.y
                    dragging = true
                }
            }
            override fun mouseReleased(e: MouseEvent) {
                dragging = false
            }
        }

        val motionListener = object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (dragging) {
                    window.setLocation(
                        startWindowX + e.xOnScreen - startScreenX,
                        startWindowY + e.yOnScreen - startScreenY
                    )
                }
            }
        }

        window.addMouseListener(mouseListener)
        window.addMouseMotionListener(motionListener)

        onDispose {
            window.removeMouseListener(mouseListener)
            window.removeMouseMotionListener(motionListener)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { windowState.isMinimized = true }) {
                    Text("−", style = MaterialTheme.typography.titleMedium)
                }
                val isMaximized = windowState.placement == WindowPlacement.Maximized
                TextButton(onClick = {
                    windowState.placement =
                        if (isMaximized) WindowPlacement.Floating else WindowPlacement.Maximized
                }) {
                    Text(if (isMaximized) "❐" else "□", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onCloseRequest) {
                    Text("✕", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
