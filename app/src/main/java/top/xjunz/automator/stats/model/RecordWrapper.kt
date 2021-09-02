package top.xjunz.automator.stats.model

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import me.zhanghai.android.appiconloader.AppIconLoader
import top.xjunz.automator.R
import top.xjunz.automator.app.AutomatorApp
import top.xjunz.automator.model.Record
import top.xjunz.automator.util.dp2px
import java.lang.ref.WeakReference

/**
 * Wrap a [Record] with its [icon] and [label].
 *
 * @author xjunz 2021/8/23
 */
data class RecordWrapper(val source: Record) {
    companion object {
        @JvmStatic
        private val iconLoader by lazy {
            AppIconLoader(dp2px(48).toInt(), true, AutomatorApp.appContext)
        }

        @JvmStatic
        private val packageManager by lazy {
            AutomatorApp.appContext.packageManager
        }
    }


    private var appInfo: WeakReference<ApplicationInfo>? = null

    private var labelLoaded = false
    private var iconLoaded = false
    private var icon: Bitmap? = null
    private var label: String? = null
    fun getFormattedDuration(): String {
        val duration = getDuration()
        val day = duration / (1_000 * 60 * 60 * 24)
        val hour = duration / (1_000 * 60 * 60) % 24
        val min = duration / (1_000 * 60) % 60
        return AutomatorApp.me.getString(R.string.format_duration, day, hour, min)
    }

    private fun getDuration() = source.latestTimestamp - source.firstTimestamp

    fun getFrequencyPerDay(): Float {
        val day = getDuration() / (1_000 * 60 * 60 * 24F)
        return source.count / day.coerceAtLeast(1f)
    }

    private fun requireAppInfo(): ApplicationInfo {
        if (appInfo == null || appInfo?.get() == null) {
            appInfo = WeakReference(packageManager.getApplicationInfo(source.pkgName, 0))
        }
        return appInfo!!.get()!!
    }

    fun getComparatorLabel(): String {
        return getLabel() ?: source.pkgName
    }

    fun getLabel(): String? {
        if (!labelLoaded) {
            try {
                label = requireAppInfo().loadLabel(packageManager).toString()
            } finally {
                labelLoaded = true
            }
        }
        return label
    }

    suspend fun loadIcon(): Bitmap? {
        if (!iconLoaded) {
            try {
                Dispatchers.IO.invoke { icon = iconLoader.loadIcon(requireAppInfo()) }
            } finally {
                iconLoaded = true
            }
        }
        return icon
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RecordWrapper

        if (source != other.source) return false

        return true
    }

    override fun hashCode(): Int {
        return source.hashCode()
    }

}