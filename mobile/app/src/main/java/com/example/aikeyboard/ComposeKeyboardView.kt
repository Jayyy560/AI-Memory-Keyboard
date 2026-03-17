package com.example.aikeyboard

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView

class ComposeKeyboardView(context: Context, private val content: @Composable () -> Unit) : AbstractComposeView(context) {
    @Composable
    override fun Content() {
        content()
    }
}
