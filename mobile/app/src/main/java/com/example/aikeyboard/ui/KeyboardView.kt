package com.example.aikeyboard.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

// ─── Theme Definitions ─────────────────────────────────────────────
data class KeyboardTheme(
    val name: String,
    val bgColor: Color,
    val keyColor: Color,
    val keyActionColor: Color,
    val textColor: Color,
    val accentColor: Color,
    val aiPanelBg: Color,
    val aiPanelText: Color,
    val popupBg: Color,
    val pressedKeyColor: Color
)

val ThemeMidnight = KeyboardTheme(
    name = "Midnight",
    bgColor = Color(0xFF121212),
    keyColor = Color(0xFF2C2C2C),
    keyActionColor = Color(0xFF1E1E1E),
    textColor = Color(0xFFE0E0E0),
    accentColor = Color(0xFF5E35B1),
    aiPanelBg = Color(0xFF1A1A2E),
    aiPanelText = Color(0xFFA9A9E2),
    popupBg = Color(0xFF484848),
    pressedKeyColor = Color(0xFF424242)
)

val ThemeOcean = KeyboardTheme(
    name = "Ocean",
    bgColor = Color(0xFF0D1B2A),
    keyColor = Color(0xFF1B2838),
    keyActionColor = Color(0xFF152238),
    textColor = Color(0xFFE0E8F0),
    accentColor = Color(0xFF1565C0),
    aiPanelBg = Color(0xFF0A2540),
    aiPanelText = Color(0xFF81D4FA),
    popupBg = Color(0xFF1E3A5F),
    pressedKeyColor = Color(0xFF2A4A6B)
)

val ThemeForest = KeyboardTheme(
    name = "Forest",
    bgColor = Color(0xFF1A1E1A),
    keyColor = Color(0xFF2A302A),
    keyActionColor = Color(0xFF1F251F),
    textColor = Color(0xFFD8E8D0),
    accentColor = Color(0xFF2E7D32),
    aiPanelBg = Color(0xFF1A2E1A),
    aiPanelText = Color(0xFFA5D6A7),
    popupBg = Color(0xFF3A503A),
    pressedKeyColor = Color(0xFF3E4E3E)
)

val ThemeRose = KeyboardTheme(
    name = "Rosé",
    bgColor = Color(0xFF1E1218),
    keyColor = Color(0xFF2E1E28),
    keyActionColor = Color(0xFF261820),
    textColor = Color(0xFFF0D8E8),
    accentColor = Color(0xFFC2185B),
    aiPanelBg = Color(0xFF2E1228),
    aiPanelText = Color(0xFFF48FB1),
    popupBg = Color(0xFF4E2E40),
    pressedKeyColor = Color(0xFF4A2E3E)
)

val ThemeSnow = KeyboardTheme(
    name = "Snow",
    bgColor = Color(0xFFE8E8E8),
    keyColor = Color(0xFFFFFFFF),
    keyActionColor = Color(0xFFCBCBCB),
    textColor = Color(0xFF333333),
    accentColor = Color(0xFF1976D2),
    aiPanelBg = Color(0xFFE3F2FD),
    aiPanelText = Color(0xFF37474F),
    popupBg = Color(0xFFFFFFFF),
    pressedKeyColor = Color(0xFFBBBBBB)
)

val allThemes = listOf(ThemeMidnight, ThemeOcean, ThemeForest, ThemeRose, ThemeSnow)

// ─── Main Keyboard ─────────────────────────────────────────────────

@Composable
fun KeyboardView(
    aiSuggestionState: String,
    suggestions: List<String>,
    onKeyPress: (String) -> Unit,
    onSaveMemoryClick: () -> Unit,
    onDeletePress: () -> Unit,
    onEnterPress: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    var isSymbolMode by remember { mutableStateOf(false) }
    var isCapsOn by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var themeIndex by remember { mutableIntStateOf(0) }
    var keyHeightScale by remember { mutableFloatStateOf(1.0f) }

    val theme = allThemes[themeIndex]
    val baseKeyHeight = (52 * keyHeightScale).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.bgColor)
            .padding(bottom = 6.dp)
    ) {
        // AI Suggestion Panel
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            shape = RoundedCornerShape(12.dp),
            color = theme.aiPanelBg,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (aiSuggestionState.isEmpty()) "✨  Type a name to get AI hints..." else "✨  $aiSuggestionState",
                    color = theme.aiPanelText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { onSaveMemoryClick() },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Word Suggestions Bar
        if (suggestions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                suggestions.take(3).forEach { word ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(theme.keyActionColor)
                            .clickable { onSuggestionClick(word) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = word,
                            color = theme.textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Settings Panel (Collapsible)
        AnimatedVisibility(
            visible = showSettings,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SettingsPanel(
                theme = theme,
                themeIndex = themeIndex,
                keyHeightScale = keyHeightScale,
                onThemeChange = { themeIndex = it },
                onScaleChange = { keyHeightScale = it }
            )
        }

        // Keyboard Keys
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            val alphaRows = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )

            val symbolRows = listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")"),
                listOf("*", "\"", "'", ":", ";", "!", "?")
            )

            val currentRows = if (isSymbolMode) symbolRows else alphaRows

            currentRows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (index == 1) Spacer(modifier = Modifier.weight(0.5f))

                    // Shift key on the third row (left side)
                    if (index == 2 && !isSymbolMode) {
                        KeyButton(
                            text = if (isCapsOn) "⇪" else "⇧",
                            modifier = Modifier.weight(1.5f),
                            theme = theme,
                            keyHeight = baseKeyHeight,
                            bgColor = if (isCapsOn) theme.accentColor else theme.keyActionColor,
                            showPopup = false
                        ) {
                            isCapsOn = !isCapsOn
                        }
                    } else if (index == 2 && isSymbolMode) {
                        Spacer(modifier = Modifier.weight(1.5f))
                    }

                    row.forEach { key ->
                        val displayKey = if (isCapsOn && !isSymbolMode && key.length == 1 && key[0].isLetter()) {
                            key.uppercase()
                        } else {
                            key
                        }
                        KeyButton(
                            text = displayKey,
                            modifier = Modifier.weight(1f),
                            theme = theme,
                            keyHeight = baseKeyHeight,
                            showPopup = true
                        ) {
                            onKeyPress(displayKey)
                        }
                    }

                    if (index == 1) Spacer(modifier = Modifier.weight(0.5f))
                    if (index == 2) {
                        // Backspace with continuous delete
                        RepeatableKeyButton(
                            text = "⌫",
                            modifier = Modifier.weight(1.5f),
                            theme = theme,
                            keyHeight = baseKeyHeight,
                            bgColor = theme.keyActionColor,
                            initialDelay = 400L,
                            repeatDelay = 50L
                        ) { onDeletePress() }
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
                KeyButton(
                    text = if (isSymbolMode) "ABC" else "?123",
                    modifier = Modifier.weight(1.5f),
                    theme = theme,
                    keyHeight = baseKeyHeight,
                    bgColor = theme.keyActionColor,
                    showPopup = false
                ) {
                    isSymbolMode = !isSymbolMode
                }

                // Settings gear button
                KeyButton(
                    text = "⚙",
                    modifier = Modifier.weight(1f),
                    theme = theme,
                    keyHeight = baseKeyHeight,
                    bgColor = theme.keyActionColor,
                    showPopup = false
                ) {
                    showSettings = !showSettings
                }

                KeyButton(
                    text = "space",
                    modifier = Modifier.weight(3.5f),
                    theme = theme,
                    keyHeight = baseKeyHeight,
                    showPopup = false
                ) { onKeyPress(" ") }

                KeyButton(
                    text = ".",
                    modifier = Modifier.weight(1f),
                    theme = theme,
                    keyHeight = baseKeyHeight,
                    bgColor = theme.keyActionColor,
                    showPopup = false
                ) { onKeyPress(".") }

                KeyButton(
                    text = "⏎",
                    modifier = Modifier.weight(1.5f),
                    theme = theme,
                    keyHeight = baseKeyHeight,
                    bgColor = theme.accentColor,
                    showPopup = false
                ) { onEnterPress() }
            }
        }
    }
}

// ─── Settings Panel ────────────────────────────────────────────────

@Composable
fun SettingsPanel(
    theme: KeyboardTheme,
    themeIndex: Int,
    keyHeightScale: Float,
    onThemeChange: (Int) -> Unit,
    onScaleChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .background(theme.aiPanelBg, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            "Theme",
            color = theme.aiPanelText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            allThemes.forEachIndexed { i, t ->
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(t.accentColor)
                        .then(
                            if (i == themeIndex) Modifier.border(2.dp, Color.White, CircleShape)
                            else Modifier
                        )
                        .pointerInput(i) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                waitForUpOrCancellation()?.let {
                                    onThemeChange(i)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t.name.take(2),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "Key Size",
            color = theme.aiPanelText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Aa", color = theme.textColor, fontSize = 13.sp)
            Slider(
                value = keyHeightScale,
                onValueChange = { onScaleChange(it) },
                valueRange = 0.8f..1.4f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor = theme.accentColor,
                    activeTrackColor = theme.accentColor,
                    inactiveTrackColor = theme.keyColor
                )
            )
            Text("AA", color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Key Button ────────────────────────────────────────────────────

@Composable
fun KeyButton(
    text: String,
    modifier: Modifier = Modifier,
    theme: KeyboardTheme,
    keyHeight: Dp = 52.dp,
    bgColor: Color = theme.keyColor,
    showPopup: Boolean = true,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .height(keyHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) theme.pressedKeyColor else bgColor)
            .pointerInput(text) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null) {
                        onClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (text.length > 1) 15.sp else 22.sp,
            color = if (bgColor == theme.accentColor) Color.White else theme.textColor,
            fontWeight = if (text.length > 1) FontWeight.Medium else FontWeight.Normal
        )

        // Key preview popup — only for printable single-char keys
        if (isPressed && showPopup && text.length == 1) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, -160),
                properties = PopupProperties(clippingEnabled = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 72.dp)
                        .shadow(8.dp, RoundedCornerShape(10.dp))
                        .background(theme.popupBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 34.sp,
                        color = theme.textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ─── Repeatable Key Button (for backspace hold-to-delete) ──────────

@Composable
fun RepeatableKeyButton(
    text: String,
    modifier: Modifier = Modifier,
    theme: KeyboardTheme,
    keyHeight: Dp = 52.dp,
    bgColor: Color = theme.keyActionColor,
    initialDelay: Long = 400L,
    repeatDelay: Long = 50L,
    onClick: () -> Unit
) {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }

    // Continuously fire onClick while the key is held down
    LaunchedEffect(isPressed) {
        if (isPressed) {
            // First fire immediately on press
            onClick()
            // Wait initial delay before repeating
            delay(initialDelay)
            // Then repeat rapidly
            while (isPressed) {
                onClick()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                delay(repeatDelay)
            }
        }
    }

    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .height(keyHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) theme.pressedKeyColor else bgColor)
            .pointerInput(text) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                    waitForUpOrCancellation()
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = theme.textColor,
            fontWeight = FontWeight.Medium
        )
    }
}
