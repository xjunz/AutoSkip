package top.xjunz.automator.app

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import rikka.shizuku.Shizuku
import top.xjunz.automator.BuildConfig

/**
 * @author xjunz 2021/6/25
 */

const val RECORD_FILE_NAME = "records"
const val LOG_FILE_NAME = "log.txt"
const val COUNT_FILE_NAME = "count"

class AutomatorApp : Application(), ViewModelStoreOwner {
    private val appViewModelStore by lazy {
        ViewModelStore()
    }

    companion object {
        lateinit var me: AutomatorApp
        lateinit var appContext: Context
        private val metrics by lazy { appContext.resources.displayMetrics }
        fun getBasicEnvInfo(): String {
            val sb = StringBuilder()
            sb.append("========Basic Env Info========")
                .append("\nSDK_INT: ${Build.VERSION.SDK_INT}")
                .append("\nSDK_RELEASE: ${Build.VERSION.RELEASE}")
                .append("\nBRAND: ${Build.BRAND}")
                .append("\nMODEL: ${Build.MODEL}")
                .append("\nVERSION_CODE: ${BuildConfig.VERSION_CODE}")
                .append("\nVERSION_NAME: ${BuildConfig.VERSION_NAME}")
                .append("\nRESOLUTION: ${metrics.widthPixels}x${metrics.heightPixels}")
                .append("\nDENSITY: ${metrics.density}")
            if (Shizuku.pingBinder()) {
                sb.append("\nSHIZUKU_VERSION: ${Shizuku.getVersion()}")
            }
            return sb.toString()
        }

        fun getScreenWidth() = metrics.widthPixels
        fun getScreenHeight() = metrics.heightPixels

    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        me = this
    }

    override fun getViewModelStore() = appViewModelStore
}