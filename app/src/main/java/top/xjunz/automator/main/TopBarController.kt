package top.xjunz.automator.main

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.InsetDrawable
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.getColorStateList
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.shape.MaterialShapeDrawable
import top.xjunz.automator.R
import top.xjunz.automator.util.dp2px
import top.xjunz.automator.util.getAttributeColor
import top.xjunz.automator.util.getAttributeColorStateList

/**
 * Control the top bar's interaction with the scroll view
 *
 * @author xjunz 2021/6/26
 */
class TopBarController(private val topBar: View, private val scrollView: NestedScrollView) {
    private val context: Context = topBar.context
    private val cornerSize by lazy {
        context.resources.getDimension(R.dimen.corner_item)
    }
    private val strokeWidthPixel = dp2px(1)
    private val zPixel = dp2px(3)
    private val margin = dp2px(8)
    private val topBarBackColor by lazy {
        getAttributeColorStateList(context, android.R.attr.colorBackground)
    }
    private val materialBack by lazy {
        MaterialShapeDrawable().apply {
            setCornerSize(cornerSize)
            strokeColor = getColorStateList(context, R.color.material_on_surface_stroke)
            strokeWidth = strokeWidthPixel
            fillColor = topBarBackColor
            requiresCompatShadow()
            setShadowColor(0xA0A0A0)
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
        }
    }
    private val insetBack by lazy {
        InsetDrawable(materialBack, 0, 0, 0, zPixel.toInt())
    }
    private val toolbarTintColor by lazy {
        getAttributeColor(context, R.attr.colorTopBarTint)
    }
    private val animator by lazy {
        ValueAnimator().apply {
            addUpdateListener {
                val f = it.animatedValue as Float
                materialBack.apply {
                    setCornerSize(cornerSize * f)
                    strokeWidth = strokeWidthPixel * f
                    elevation = zPixel * (1 - f)
                    fillColor = ColorStateList.valueOf(evaluator.evaluate((1 - f), topBarBackColor.defaultColor, toolbarTintColor) as Int)
                }
                topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    marginStart = (f * margin).toInt()
                    marginEnd = (f * margin).toInt()
                }
            }
            duration = 168
            interpolator = FastOutSlowInInterpolator()
        }
    }
    private var lastScrollY = 0
    private val evaluator = ArgbEvaluator()

    fun init() {
        topBar.apply {
            bringToFront()
            background = insetBack
            setOnApplyWindowInsetsListener { _, insets ->
                val sysInsets = WindowInsetsCompat.toWindowInsetsCompat(insets).getInsets(WindowInsetsCompat.Type.systemBars())
                OneShotPreDrawListener.add(this) {
                    scrollView.setPadding(0, height, 0, (sysInsets.bottom + margin).toInt())
                }
                setPadding(0, sysInsets.top, 0, zPixel.toInt())
                return@setOnApplyWindowInsetsListener insets
            }
        }

        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (lastScrollY < margin && scrollY >= margin) {
                elevate()
            } else if (lastScrollY > margin && scrollY <= margin) {
                flatten()
            }
            lastScrollY = scrollY
        })
    }

    private fun ensureAnimatorContinuity(): Float? {
        if (animator.isRunning) {
            animator.cancel()
            return animator.animatedValue as Float
        }
        return null
    }

    private fun elevate() = animator.apply {
        setFloatValues(ensureAnimatorContinuity() ?: 1f, 0f)
        start()
    }


    private fun flatten() = animator.apply {
        setFloatValues(ensureAnimatorContinuity() ?: 0f, 1f)
        start()
    }
}