package top.xjunz.automator.test

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.math.MathUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.customview.widget.ViewDragHelper
import top.xjunz.automator.R


/**
 * @author xjunz 2021/7/10
 */
class TestLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
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
    private val skipTarget: TextView by lazy {
        findViewById(R.id.skip_target)
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
    private var curPanelTop = 0
    private var curTargetPosition: Point? = null

    private var viewDragHelper: ViewDragHelper
    private val viewDragHelperCallback = object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == bottomPanel || child == skipTarget
        }

        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            curPanelTop = bottomPanel.top
            if (curTargetPosition == null) {
                curTargetPosition = Point()
            }
            curTargetPosition!!.set(skipTarget.left, skipTarget.top)
            if (changedView == bottomPanel) {
                if (top <= skipTarget.bottom) {
                    skipTarget.let {
                        it.layout(it.left, top - it.height, it.right, top)
                    }
                }
            }
        }

        override fun getViewVerticalDragRange(child: View) = when (child) {
            bottomPanel -> maxTop - minTop - handlerHeight
            skipTarget -> bottomPanel.top
            else -> height
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            if (child == skipTarget) {
                return MathUtils.clamp(left, 0, width - child.width)
            }
            return super.clampViewPositionHorizontal(child, left, dx)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int) = when (child) {
            bottomPanel -> MathUtils.clamp(top, minTop, maxTop)
            skipTarget -> MathUtils.clamp(top, 0, bottomPanel.top - skipTarget.height)
            else -> top
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
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
        if (curPanelTop == 0) {
            curPanelTop = maxTop
        }
        bottomPanel.layout(0, curPanelTop, bottomPanel.right, curPanelTop + bottomPanel.height)
        curTargetPosition?.run {
            skipTarget.layout(x, y, x + skipTarget.width, y + skipTarget.height)
        }
    }

    private val initialWidth by lazy {
        skipTarget.width
    }
    private val initialHeight by lazy {
        skipTarget.height
    }

    fun adjustTargetWidth(fraction: Float) {
        skipTarget.run {
            val nextWidth = ((fraction * 2 + 1) * initialWidth).toInt()
            val parentWidth = this@TestLayout.width
            width = nextWidth
            if (left + nextWidth - parentWidth > 0) {
                curTargetPosition!!.x = parentWidth - nextWidth
                skipTarget.requestLayout()
            }
        }
    }

    fun adjustTargetHeight(fraction: Float) {
        skipTarget.run {
            val nextHeight = ((fraction * 3 + 1) * initialHeight).toInt()
            height = nextHeight
            if (top + nextHeight - bottomPanel.top > 0) {
                curTargetPosition!!.y = bottomPanel.top - nextHeight
                skipTarget.requestLayout()
            }
        }
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