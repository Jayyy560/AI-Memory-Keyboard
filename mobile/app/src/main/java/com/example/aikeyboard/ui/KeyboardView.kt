package com.example.aikeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            .background(Color(0xFFEEEEEE))
    ) {
        // AI Suggestion Panel (The "Contextual Hint" layer)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE3F2FD),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (aiSuggestionState.isEmpty()) "AI: Type a name or topic..." else aiSuggestionState,
                    color = Color.DarkGray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { onSaveMemoryClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Save Memory", fontSize = 12.sp)
                }
            }
        }

        // Standard Keyboard Keys Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            val rows = listOf(
                listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                listOf("Z", "X", "C", "V", "B", "N", "M")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    row.forEach { key ->
                        KeyButton(text = key, modifier = Modifier.weight(1f)) {
                            onKeyPress(key)
                        }
                    }
                }
            }

            // Bottom control row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                KeyButton(text = "DEL", modifier = Modifier.weight(1.5f)) { onDeletePress() }
                KeyButton(text = "SPACE", modifier = Modifier.weight(3f)) { onKeyPress(" ") }
                KeyButton(text = "ENTER", modifier = Modifier.weight(1.5f)) { onEnterPress() }
            }
        }
    }
}

@Composable
fun KeyButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .height(50.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 18.sp, color = Color.Black)
    }
}
