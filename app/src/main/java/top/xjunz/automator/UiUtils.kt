package top.xjunz.automator

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.util.TypedValue
import androidx.collection.SparseArrayCompat

/**
 * @author xjunz 2021/6/25
 */
private val appContext by lazy {
    AutomatorApp.appContext
}

private val cachedAttributes by lazy {
    SparseArrayCompat<Any>()
}

fun dp2px(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, appContext.resources.displayMetrics)
fun dp2px(dp: Int) = dp2px(dp.toFloat())

private inline fun <reified R : Any> cacheFirst(attr: Int, block: () -> R) =
    cachedAttributes.get(attr) as R? ?: block.invoke().also { cachedAttributes.put(attr, it) }

fun getAttributeColorStateList(context: Context, attr: Int): ColorStateList = cacheFirst(attr) {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return context.getColorStateList(value.resourceId)
}

fun getAttributeColor(context: Context, attr: Int): Int = cacheFirst(attr) {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return context.getColor(value.resourceId)
}

fun getAttributeColorAsList(context: Context, attr: Int): ColorStateList {
    return ColorStateList.valueOf(getAttributeColor(context, attr))
}