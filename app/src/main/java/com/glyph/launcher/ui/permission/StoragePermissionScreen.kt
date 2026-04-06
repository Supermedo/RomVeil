package com.glyph.launcher.ui.permission

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import com.glyph.launcher.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.glyph.launcher.ui.theme.*

/**
 * Checks if the app has All Files Access (MANAGE_EXTERNAL_STORAGE) on Android 11+.
 * This is required so emulators can read ROM files passed via file:// URIs.
 *
 * On Android 10 and below, this screen is skipped automatically.
 */
@Composable
fun StoragePermissionScreen(
    onPermissionGranted: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { mutableStateOf(checkStoragePermission()) }

    BackHandler(enabled = onBack != null) { onBack?.invoke() }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasPermission = checkStoragePermission()
            if (hasPermission) {
                onPermissionGranted()
            }
        }
    }

    if (hasPermission) {
        LaunchedEffect(Unit) { onPermissionGranted() }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlyphBlack)
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp)
                    .background(GlyphWhiteDisabled, RoundedCornerShape(4.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "BACK",
                    style = PlatformLabel.copy(color = GlyphWhiteMedium)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "RomVeil Logo",
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "RomVeil",
                style = GameTitleSelected.copy(letterSpacing = GlyphTypography.labelLarge.letterSpacing)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "STORAGE PERMISSION REQUIRED",
                style = PlatformLabel.copy(color = GlyphDimmed)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "RomVeil needs \"All Files Access\" so emulators can read your ROM files.\n\nTap below, find RomVeil in the list, and enable the toggle.",
                style = GameMetadata.copy(color = GlyphWhiteLow),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .background(GlyphWhiteHigh, RoundedCornerShape(4.dp))
                    .clickable {
                        openAllFilesAccessSettings(context)
                    }
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OPEN SETTINGS",
                    style = PlatformLabel.copy(color = GlyphBlack)
                )
            }
        }
    }
}

/**
 * Check if we have all-files access.
 * Returns true on Android 10 and below (no restriction), or if granted on 11+.
 */
fun checkStoragePermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true // Android 10 and below — requestLegacyExternalStorage handles it
    }
}

private fun openAllFilesAccessSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            // Try to open directly to our app's permission page
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback: open the general all-files-access settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Last resort: open app info
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
}
