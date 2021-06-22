package top.xjunz.library.automator

import `$android`.hardware.input.InputManager
import android.content.ComponentName
import android.view.InputEvent

/**
 * Abstract automator entity, which defines an automator's abilities.
 * Obtain an instance via [AutomatorFactory.getAutomator].
 *
 * @author xjunz 2021/6/20 22:26
 */
abstract class Automator protected constructor() {
    companion object {
        /**
         * Input Event Injection Synchronization Mode: None.
         * Never blocks.  Injection is asynchronous and is assumed always to be successful.
         */
        const val INJECT_INPUT_EVENT_MODE_ASYNC = 0 // see InputDispatcher.h


        /**
         * Input Event Injection Synchronization Mode: Wait for result.
         * Waits for previous events to be dispatched so that the input dispatcher can
         * determine whether input event injection will be permitted based on the current
         * input focus.  Does not wait for the input event to finish being handled
         * by the application.
         */
        const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1 // see InputDispatcher.h


        /**
         * Input Event Injection Synchronization Mode: Wait for finish.
         * Waits for the event to be delivered to the application and handled.
         */
        const val INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2 // see InputDispatcher.h
    }

    abstract fun performClick(x: Float, y: Float)
    abstract fun performActionUp(x: Float, y: Float)
    abstract fun performActionDown(x: Float, y: Float)
    abstract fun injectInputEvent(event: InputEvent, mode: Int): Boolean
    abstract fun getForegroundComponentName(): ComponentName?
}