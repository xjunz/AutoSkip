package top.xjunz.automator

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.customview.widget.ViewDragHelper


/**
 * @author xjunz 2021/7/10
 */
class TestLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val handler: View by lazy {
        findViewById(R.id.handler)
    }
    private val bottomBar: View by lazy {
        findViewById(R.id.bottom_bar)
    }
    private val bottomPanel: View by lazy {
        findViewById(R.id.bottom_panel)
    }

    private val handlerHeight by lazy {
        (handler.layoutParams as MarginLayoutParams).run {
            height + topMargin + bottomMargin
        }
    }

    private val maxTop by lazy {
        height - handlerHeight - bottomBar.height
    }
    private val minTop by lazy {
        height - bottomPanel.height - bottomBar.height
    }
    private var curTop = 0

    private var viewDragHelper: ViewDragHelper
    private val viewDragHelperCallback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int) = child != bottomBar
        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            curTop = bottomPanel.top
        }

        override fun getViewVerticalDragRange(child: View) = when (child) {
            bottomPanel -> maxTop - minTop
            else -> height
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (child != bottomPanel) {
                return left
            }
            return super.clampViewPositionHorizontal(child, left, dx)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int) = when (child) {
            bottomPanel -> MathUtils.clamp(top, minTop, maxTop)
            else -> top
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            super.onViewReleased(releasedChild, xvel, yvel)
            if (releasedChild == bottomPanel) {
                if (yvel > 0) {
                    closePanel()
                } else if (yvel < 0) {
                    openPanel()
                } else {
                    if ((releasedChild.top.toFloat() - minTop) / (maxTop - minTop) > .5f) {
                        closePanel()
                    } else {
                        openPanel()
                    }
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (curTop == 0) {
            curTop = maxTop
        }
        bottomPanel.layout(0, curTop, bottomPanel.right, curTop + bottomPanel.height)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        viewDragHelper.processTouchEvent(event!!)
        return true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = viewDragHelper.shouldInterceptTouchEvent(ev!!)

    init {
        viewDragHelper = ViewDragHelper.create(this, viewDragHelperCallback)
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            bottomBar.apply {
                setPadding(paddingLeft, paddingTop, paddingRight,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
            }
            return@setOnApplyWindowInsetsListener insets
        }
    }

    fun openPanel() {
        if (viewDragHelper.smoothSlideViewTo(bottomPanel, 0, minTop)) {
            postInvalidateOnAnimation()
        }
    }

    fun closePanel() {
        if (viewDragHelper.smoothSlideViewTo(bottomPanel, 0, maxTop)) {
            postInvalidateOnAnimation()
        }
    }

    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            postInvalidateOnAnimation()
        }
    }
}