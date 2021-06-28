package top.xjunz.automator

import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.util.StateSet
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import androidx.cardview.widget.CardView
import androidx.databinding.BindingAdapter
import com.google.android.material.button.MaterialButton

/**
 * @author xjunz 2021/6/26
 */
@BindingAdapter("tooltip")
fun View.setTooltip(text: CharSequence) {
    TooltipCompat.setTooltipText(this, text)
    contentDescription = text
}