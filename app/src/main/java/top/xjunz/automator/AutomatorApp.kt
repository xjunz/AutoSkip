package top.xjunz.automator

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * @author xjunz 2021/6/25
 */
val config by lazy { AutomatorApp.Config() }

class AutomatorApp : Application(), ViewModelStoreOwner {
    private val appViewModelStore by lazy {
        ViewModelStore()
    }

    companion object {
        lateinit var appContext: Context
        private lateinit var sharedPreferences: SharedPreferences
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
        const val SP_CONFIG_NAME = "config"
    }

    class Config {
        companion object {
            private const val key_fallback_injecting_events = "fallback_injecting_events"
            private const val key_region_aware = "region_aware"
        }

        fun shouldFallbackInjectingEvents() = sharedPreferences.getBoolean(key_fallback_injecting_events, false)

        fun setFallbackInjectingEvents(running: Boolean) = sharedPreferences.edit().putBoolean(key_fallback_injecting_events, running).apply()

    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        sharedPreferences =
            applicationContext.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE)
    }

    override fun getViewModelStore() = appViewModelStore
}