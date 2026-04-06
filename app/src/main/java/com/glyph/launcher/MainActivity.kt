package com.glyph.launcher

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import dagger.hilt.android.AndroidEntryPoint
import com.glyph.launcher.ui.GlyphApp
import com.glyph.launcher.ui.theme.GlyphTheme

/**
 * CompositionLocal to propagate hardware key events into Compose.
 */
val LocalKeyEventDispatcher = staticCompositionLocalOf<((KeyEvent) -> Boolean)?> { null }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var keyEventListener: ((KeyEvent) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CompositionLocalProvider(LocalKeyEventDispatcher provides keyEventListener) {
                GlyphTheme {
                    GlyphApp()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (keyEventListener?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    fun setKeyEventListener(listener: ((KeyEvent) -> Boolean)?) {
        keyEventListener = listener
    }
}
