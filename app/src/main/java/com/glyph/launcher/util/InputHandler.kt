package com.glyph.launcher.util

import android.view.KeyEvent

/**
 * Translates physical controller input (D-Pad, gamepad buttons)
 * into semantic actions for the Glyph UI.
 */
object InputHandler {

    enum class Action {
        NAVIGATE_UP,
        NAVIGATE_DOWN,
        NAVIGATE_LEFT,
        NAVIGATE_RIGHT,
        CONFIRM,           // A button / Enter
        BACK,              // B button / Back
        MENU,              // Start / Menu button
        OPTIONS,           // Y button — ROM options (rescrape, delete, etc.)
        SEARCH,            // X button — toggle search
        TRIGGER_LEFT,      // L1 / LB — platform switch
        TRIGGER_RIGHT,     // R1 / RB — platform switch
        TOGGLE_FAVORITE,   // Select button
        NONE
    }

    /**
     * Map a raw KeyEvent to a semantic Glyph Action.
     * Only processes KEY_DOWN events to avoid double-firing.
     */
    fun mapKeyEvent(event: KeyEvent): Action {
        if (event.action != KeyEvent.ACTION_DOWN) return Action.NONE

        return when (event.keyCode) {
            // D-Pad
            KeyEvent.KEYCODE_DPAD_UP -> Action.NAVIGATE_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Action.NAVIGATE_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> Action.NAVIGATE_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Action.NAVIGATE_RIGHT

            // Confirm (A button / face button south)
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> Action.CONFIRM

            // Back (B button / face button east)
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE -> Action.BACK

            // Menu (Start button)
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_MENU -> Action.MENU

            // Options (Y button) — ROM context menu
            KeyEvent.KEYCODE_BUTTON_Y -> Action.OPTIONS

            // Search (X button) — toggle search bar
            KeyEvent.KEYCODE_BUTTON_X -> Action.SEARCH

            // Select (Select button) — toggle favorite
            KeyEvent.KEYCODE_BUTTON_SELECT -> Action.TOGGLE_FAVORITE

            // Shoulder buttons for platform switching
            KeyEvent.KEYCODE_BUTTON_L1 -> Action.TRIGGER_LEFT
            KeyEvent.KEYCODE_BUTTON_R1 -> Action.TRIGGER_RIGHT

            else -> Action.NONE
        }
    }

    /**
     * Check if a key event is one we handle (to consume it and prevent default behavior).
     */
    fun isHandledKey(keyCode: Int): Boolean {
        return keyCode in setOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
        )
    }
}
