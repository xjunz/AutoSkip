package top.xjunz.library.automator

/**
 * Abstract automator entity which defines automators' abilities.
 *
 * @author xjunz 2021/6/20 22:26
 */
abstract class Automator protected constructor(){
    abstract fun performClick(x: Float, y: Float)
}