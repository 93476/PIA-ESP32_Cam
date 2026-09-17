package mx.conalep.pia

import android.content.Context

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var piaBaseUrl: String
        get() = preferences.getString(KEY_PIA_URL, DEFAULT_PIA_URL) ?: DEFAULT_PIA_URL
        set(value) {
            preferences.edit().putString(KEY_PIA_URL, DeviceUrls.normalizeBaseUrl(value)).apply()
        }

    var cameraBaseUrl: String
        get() = preferences.getString(KEY_CAMERA_URL, DEFAULT_CAMERA_URL) ?: DEFAULT_CAMERA_URL
        set(value) {
            preferences.edit().putString(KEY_CAMERA_URL, DeviceUrls.normalizeBaseUrl(value)).apply()
        }

    var phoneNumber: String
        get() = preferences.getString(KEY_PHONE, "") ?: ""
        set(value) {
            preferences.edit().putString(KEY_PHONE, value.trim()).apply()
        }

    companion object {
        const val DEFAULT_PIA_URL = "http://192.168.4.1"
        const val DEFAULT_CAMERA_URL = "http://192.168.4.2"

        private const val PREFS_NAME = "pia_monitor_settings"
        private const val KEY_PIA_URL = "pia_base_url"
        private const val KEY_CAMERA_URL = "camera_base_url"
        private const val KEY_PHONE = "phone_number"
    }
}
