package top.xjunz.library.automator

import top.xjunz.library.automator.impl.RootAutomator
import top.xjunz.library.automator.impl.ShizukuAutomator

/**
 * A factory entity, which provides an [Automator]'s instance according to the [Mode] passed in.
 * @see Mode
 *
 * @author xjunz 2021/6/21
 */
class AutomatorFactory {
    sealed class Mode(val name: String) {
        object ROOT : Mode("root")
        object SHIZUKU : Mode("shizuku")
        object ACCESSIBILITY : Mode("accessibility")
    }

    companion object {
        fun getAutomator(mode: Mode): Automator =
            when (mode) {
                Mode.ACCESSIBILITY -> TODO()
                Mode.ROOT -> RootAutomator.INSTANCE
                Mode.SHIZUKU -> ShizukuAutomator.INSTANCE
            }
    }
}