package com.remodex.mobile

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.remodex.mobile.core.model.AppLanguagePreference
import com.remodex.mobile.core.model.AppThemePreference
import com.remodex.mobile.data.QrPairingValidationResult
import com.remodex.mobile.data.validatePairingQrCode
import com.remodex.mobile.data.LanguagePreferences
import com.remodex.mobile.data.ThemePreferences
import com.remodex.mobile.core.notification.RemodexLocalNotificationPresenter
import com.remodex.mobile.ui.LocalAIChangeSetPersistence
import com.remodex.mobile.ui.LocalCodexRepository
import com.remodex.mobile.ui.RootScreen
import com.remodex.mobile.ui.theme.RemodexTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LanguagePreferences.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationLaunchIntent(intent)
        handleDebugPairingIntent(intent)
        setContent {
            val context = LocalContext.current
            var themePref by remember { mutableStateOf(ThemePreferences.read(context)) }
            val systemDark = isSystemInDarkTheme()
            DisposableEffect(context) {
                val prefs =
                    context.applicationContext.getSharedPreferences(
                        ThemePreferences.PREFS_NAME,
                        android.content.Context.MODE_PRIVATE,
                    )
                val listener =
                    android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                        if (key == AppThemePreference.storageKey) {
                            themePref = ThemePreferences.read(context)
                        } else if (key == AppLanguagePreference.storageKey) {
                            recreate()
                        }
                    }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose {
                    prefs.unregisterOnSharedPreferenceChangeListener(listener)
                }
            }
            val darkTheme = themePref.isDark(systemDark)
            CompositionLocalProvider(
                LocalCodexRepository provides AppContainer.codexRepository,
                LocalAIChangeSetPersistence provides AppContainer.aiChangeSetPersistence,
            ) {
                RemodexTheme(darkTheme = darkTheme) {
                    RootScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationLaunchIntent(intent)
        handleDebugPairingIntent(intent)
    }

    private fun handleNotificationLaunchIntent(intent: Intent?) {
        val tid =
            intent?.getStringExtra(RemodexLocalNotificationPresenter.EXTRA_THREAD_ID)?.trim()
                ?: return
        if (tid.isNotEmpty() && RemodexLocalNotificationPresenter.consumeLaunchToken(this, intent, tid)) {
            AppContainer.setPendingOpenThreadFromNotification(tid)
        }
    }

    private fun handleDebugPairingIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        val raw = readDebugPairingJson(intent)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return
        when (val result = validatePairingQrCode(raw)) {
            is QrPairingValidationResult.Success -> {
                AppContainer.sessionPersistence.applyPairingPayload(
                    payload = result.payload,
                    secureStore = AppContainer.secureStore,
                )
                AppContainer.sessionPersistence.saveLocalRelayHostOverride(null)
            }
            is QrPairingValidationResult.ScanError ->
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            is QrPairingValidationResult.BridgeUpdateRequired ->
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val EXTRA_DEBUG_PAIRING_JSON = "remodex_pairing_json"
        const val EXTRA_DEBUG_PAIRING_JSON_B64 = "remodex_pairing_json_b64"
        const val DEBUG_PAIRING_FILE_NAME = "debug-pairing.json"
    }

    private fun readDebugPairingJson(intent: Intent?): String? {
        intent?.getStringExtra(EXTRA_DEBUG_PAIRING_JSON)?.let { return it }
        val encoded = intent?.getStringExtra(EXTRA_DEBUG_PAIRING_JSON_B64)?.trim()?.takeIf { it.isNotEmpty() }
        if (encoded != null) {
            return runCatching {
                String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
            }.getOrNull()
        }
        val file = File(filesDir, DEBUG_PAIRING_FILE_NAME)
        if (!file.isFile) return null
        return runCatching {
            file.readText().also { file.delete() }
        }.getOrNull()
    }
}
