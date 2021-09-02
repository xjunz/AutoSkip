package top.xjunz.automator.util

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.View
import androidx.appcompat.widget.TooltipCompat
import androidx.collection.SparseArrayCompat
import androidx.databinding.BindingAdapter
import top.xjunz.automator.app.AutomatorApp

/**
 * @author xjunz 2021/6/25
 */
@BindingAdapter("tooltip")
fun View.setTooltip(text: CharSequence) {
    TooltipCompat.setTooltipText(this, text)
    contentDescription = text
}

@BindingAdapter("visible")
fun View.setVisible(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

private val cachedAttributes by lazy {
    SparseArrayCompat<Any>(5)
}

fun dp2px(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, AutomatorApp.appContext.resources.displayMetrics)
fun dp2px(dp: Int) = dp2px(dp.toFloat())

private inline fun <reified R : Any> cacheWhenNecessary(attr: Int, block: () -> R) =
    cachedAttributes.get(attr) as R? ?: block.invoke().also { cachedAttributes.put(attr, it) }

fun getAttributeColorStateList(context: Context, attr: Int): ColorStateList = cacheWhenNecessary(attr) {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return context.getColorStateList(value.resourceId)
}

fun getAttributeColor(context: Context, attr: Int): Int = cacheWhenNecessary(attr) {
    val value = TypedValue()
    context.theme.resolveAttribute(attr, value, true)
    return context.getColor(value.resourceId)
}

fun getAttributeColorAsList(context: Context, attr: Int): ColorStateList {
    return ColorStateList.valueOf(getAttributeColor(context, attr))
}