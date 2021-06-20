package top.xjunz.library.automator.impl

import android.hardware.input.HiddenInputManager
import android.os.SystemClock
import android.view.MotionEvent
import top.xjunz.library.automator.Automator

/**
 * An [Automator] implemented with root(superuser) permission.
 * @author xjunz 2021/6/20 22:28
 */
open class RootAutomator : Automator() {
    object INSTANCE : RootAutomator()

    override fun performClick(x: Float, y: Float) {
        //Instrumentation().sendPointerSync()
        HiddenInputManager.getInstance().injectInputEvent(null, 0);
        val event = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0
        )
        //InputManagerBridge.injectInputEvent(event,InputManagerBridge.getInputManager().INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
    }
}