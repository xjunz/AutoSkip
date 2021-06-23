package top.xjunz.library.automator.impl

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import `$android`.hardware.input.InputManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.HandlerThread
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import rikka.shizuku.Shizuku
import top.xjunz.library.automator.IAutomatorConnection
import top.xjunz.library.automator.IOnAccessibilityEventListener
import java.util.*
import kotlin.system.exitProcess


/**
 * @author xjunz 2021/6/22 23:01
 */
class AutomatorConnection : IAutomatorConnection.Stub() {

    companion object {
        private const val HANDLER_THREAD_NAME = "AutomatorHandlerThread"
        const val TAG = "automator"
    }

    private val mHandlerThread = HandlerThread(HANDLER_THREAD_NAME)

    private lateinit var mUiAutomation: UiAutomation

    override fun connect() {
        check(!mHandlerThread.isAlive) { "Already connected!" }
        mHandlerThread.start()
        mUiAutomation = UiAutomation(mHandlerThread.looper, UiAutomationConnection())
        mUiAutomation.connect()
        startMonitor()
    }

    private var lastEvent: AccessibilityEvent? = null
    private fun startMonitor() {
        mUiAutomation.serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        mUiAutomation.setOnAccessibilityEventListener {
            if (Objects.equals(it, lastEvent)) {
                return@setOnAccessibilityEventListener
            }
            lastEvent = it
            val windowInfo = mUiAutomation.rootInActiveWindow ?: return@setOnAccessibilityEventListener
            windowInfo.findAccessibilityNodeInfosByText("跳过")?.forEach { node ->
                Log.i("automator", node.toString())
                val text = node.text
                if (text.contains(Regex("\\d"))) {
                    if (node.isClickable) {
                        Log.i("automator", "Skipped!")
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else {
                        //fallback
                        Log.i("automator", "Fallback!")
                        val downTime = SystemClock.uptimeMillis()
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        val downAction = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, rect.exactCenterX(), rect.exactCenterY(), 0)
                        downAction.source = InputDevice.SOURCE_TOUCHSCREEN
                        mUiAutomation.injectInputEvent(downAction, true)
                        val upAction = MotionEvent.obtain(downAction).apply { action = MotionEvent.ACTION_UP }
                        mUiAutomation.injectInputEvent(upAction, true)
                        upAction.recycle()
                        downAction.recycle()
                    }
                }
            }
        }
    }

    override fun disconnect() {
        check(mHandlerThread.isAlive) { "Already disconnected!" }
        mUiAutomation.disconnect()
        mHandlerThread.quit()
    }

    override fun takeScreenshot(crop: Rect?, rotation: Int): Bitmap? = mUiAutomation.takeScreenshot()

    override fun shutdown() = exitProcess(0)

    override fun setOnAccessibilityEventListener(client: IOnAccessibilityEventListener?) =
        mUiAutomation.setOnAccessibilityEventListener { event -> client!!.onAccessibilityEvent(event) }

    override fun sayHello() = "Hello from remote server! My uid is ${Process.myUid()}"

    override fun isConnnected() = mHandlerThread.isAlive

    override fun getRootInActiveWindow(): AccessibilityNodeInfo = mUiAutomation.rootInActiveWindow

    fun setCompressedLayoutHierarchy(compressed: Boolean) {
        val info = mUiAutomation.serviceInfo
        if (compressed) info.flags = info.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv() else info.flags = info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        mUiAutomation.serviceInfo = info
    }
}