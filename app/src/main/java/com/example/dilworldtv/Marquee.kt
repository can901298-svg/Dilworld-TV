package com.example.dilworldtv

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    speedDpPerSec: Float = 140f,
    style: TextStyle = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFE6B0)),
    gap: String = "   •   "
) {
    var containerWidthPx by remember { mutableStateOf(0) }
    var textWidthPx by remember { mutableStateOf(0) }
    var xPx by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val speedPxPerMs = with(density) { speedDpPerSec.dp.toPx() } / 1000f

    val loopText = remember(text) { text + gap + text }

    LaunchedEffect(containerWidthPx, textWidthPx, loopText) {
        if (containerWidthPx <= 0 || textWidthPx <= 0) return@LaunchedEffect
        xPx = 0f
        while (true) {
            xPx -= speedPxPerMs * 16f
            if (xPx <= -textWidthPx / 2f) xPx = 0f
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { containerWidthPx = it.width }
    ) {
        Text(
            text = loopText,
            style = style,
            modifier = Modifier
                .onSizeChanged { textWidthPx = it.width }
                .offset { IntOffset(xPx.roundToInt(), 0) }
        )
    }
}
