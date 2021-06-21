package top.xjunz.library.automator.impl

import `$android`.hardware.input.InputManager
import android.os.SystemClock
import android.view.InputEvent
import android.view.MotionEvent
import androidx.core.view.InputDeviceCompat
import top.xjunz.library.automator.Automator

/**
 * An [Automator] implemented with root(superuser) permission.
 *
 * @author xjunz 2021/6/20 22:28
 */
open class RootAutomator private constructor() : Automator() {
    internal object INSTANCE : RootAutomator()

    override fun performClick(x: Float, y: Float) {
        val event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN, x, y, 0)
        event.source = InputDeviceCompat.SOURCE_TOUCHSCREEN
        inputManager.injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
        event.recycle()
    }
}