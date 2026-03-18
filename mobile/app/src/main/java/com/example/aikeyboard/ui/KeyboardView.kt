package com.example.aikeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Modern Color Palette
val BgColor = Color(0xFF121212)
val KeyNormalColor = Color(0xFF2C2C2C)
val KeyActionColor = Color(0xFF1E1E1E)
val TextColor = Color(0xFFE0E0E0)
val AiPanelBgColor = Color(0xFF1A1A2E)
val AiPanelTextColor = Color(0xFFA9A9E2)
val AccentColor = Color(0xFF5E35B1)

@Composable
fun KeyboardView(
    aiSuggestionState: String,
    onKeyPress: (String) -> Unit,
    onSaveMemoryClick: () -> Unit,
    onDeletePress: () -> Unit,
    onEnterPress: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgColor)
            .padding(bottom = 8.dp) // padding for gesture bars
    ) {
        // AI Suggestion Panel (The "Contextual Hint" layer)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = AiPanelBgColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (aiSuggestionState.isEmpty()) "✨  No memories found. Type a name..." else "✨  $aiSuggestionState",
                    color = AiPanelTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { onSaveMemoryClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .height(34.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Standard Keyboard Keys Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val rows = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )

            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Add padding to middle rows for classic staggered layout
                    if (index == 1) Spacer(modifier = Modifier.weight(0.5f))
                    if (index == 2) Spacer(modifier = Modifier.weight(1.5f))

                    row.forEach { key ->
                        KeyButton(text = key, modifier = Modifier.weight(1f)) {
                            onKeyPress(key)
                        }
                    }

                    if (index == 1) Spacer(modifier = Modifier.weight(0.5f))
                    if (index == 2) {
                        KeyButton(text = "⌫", modifier = Modifier.weight(1.5f), bgColor = KeyActionColor) { onDeletePress() }
                    }
                }
            }

            // Bottom control row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                KeyButton(text = "?123", modifier = Modifier.weight(1.5f), bgColor = KeyActionColor) { /* TBD */ }
                KeyButton(text = ",", modifier = Modifier.weight(1f), bgColor = KeyActionColor) { onKeyPress(",") }
                KeyButton(text = "space", modifier = Modifier.weight(4f)) { onKeyPress(" ") }
                KeyButton(text = ".", modifier = Modifier.weight(1f), bgColor = KeyActionColor) { onKeyPress(".") }
                KeyButton(text = "⏎", modifier = Modifier.weight(1.5f), bgColor = AccentColor) { onEnterPress() }
            }
        }
    }
}

@Composable
fun KeyButton(
    text: String, 
    modifier: Modifier = Modifier, 
    bgColor: Color = KeyNormalColor,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 5.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White)
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            fontSize = if (text.length > 1) 15.sp else 21.sp, 
            color = if (bgColor == AccentColor) Color.White else TextColor,
            fontWeight = if (text.length > 1) FontWeight.Medium else FontWeight.Normal
        )
    }
}
