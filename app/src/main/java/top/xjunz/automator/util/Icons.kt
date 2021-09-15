package top.xjunz.automator.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import me.zhanghai.android.appiconloader.AppIconLoader
import top.xjunz.automator.app.AutomatorApp

/**
 * @author xjunz 2021/9/15
 */
val iconLoader by lazy {
    AppIconLoader(dp2px(48).toInt(), true, AutomatorApp.appContext)
}
val myIcon by lazy {
    iconLoader.loadIcon(AutomatorApp.appContext.applicationInfo)
}
val desaturatedMyIconDrawable by lazy {
    BitmapDrawable(AutomatorApp.appContext.resources, myIcon).apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0F) })
    }
}