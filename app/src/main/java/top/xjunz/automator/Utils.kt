package top.xjunz.automator

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue

/**
 * @author xjunz 2021/6/25
 */
class Utils {
    companion object {
        private val appContext by lazy {
            AutomatorApp.appContext
        }

        fun dp2px(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, appContext.resources.displayMetrics)
        fun dp2px(dp: Int) = dp2px(dp.toFloat())
        fun getAttributeColorStateList(context: Context, attr: Int): ColorStateList? {
            val value = TypedValue()
            context.theme.resolveAttribute(attr, value, true)
            if (value.resourceId != 0) {
                return context.getColorStateList(value.resourceId)
            }
            return null
        }
    }
}