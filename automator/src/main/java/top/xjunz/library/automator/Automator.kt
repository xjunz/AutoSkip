package top.xjunz.library.automator

import `$android`.hardware.input.InputManager

/**
 * Abstract automator entity, which defines an automator's abilities.
 * Obtain an instance via [AutomatorFactory.getAutomator].
 *
 * @author xjunz 2021/6/20 22:26
 */
abstract class Automator protected constructor() {
    protected val inputManager: InputManager by lazy {
        InputManager.getInstance()
    }

    abstract fun performClick(x: Float, y: Float)
}