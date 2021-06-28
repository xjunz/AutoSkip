package top.xjunz.automator

import android.app.Application
import android.content.Context
import android.content.SharedPreferences

/**
 * @author xjunz 2021/6/25
 */
val config by lazy { AutomatorApp.Config() }

class AutomatorApp : Application() {
    companion object {
        lateinit var appContext: Context
        private lateinit var sharedPreferences: SharedPreferences
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
        const val SP_CONFIG_NAME = "config"
    }

    class Config {
        companion object {
            private const val key_last_running = "last_running"
        }

        fun isLastRunning() = sharedPreferences.getBoolean(key_last_running, false)

        fun recordRunningState(running: Boolean) = sharedPreferences.edit().putBoolean(key_last_running, running).apply()
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        sharedPreferences =
            applicationContext.getSharedPreferences(SP_CONFIG_NAME, Context.MODE_PRIVATE)
    }
}