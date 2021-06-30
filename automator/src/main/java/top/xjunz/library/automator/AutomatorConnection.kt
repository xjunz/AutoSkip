package top.xjunz.library.automator

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.*
import android.system.Os
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    private var config: AutomatorConfig = AutomatorConfig().apply {
        fallbackInjectingEvents = true
        detectRegion = true
    }

    private val handlerThread = HandlerThread(HANDLER_THREAD_NAME)
    private var startTimestamp = -1L
    private lateinit var uiAutomation: UiAutomation
    override fun connect() {
        Log.i(TAG, "v12")
        check(!handlerThread.isAlive) { "Already connected!" }
        handlerThread.start()
        uiAutomation = UiAutomation(handlerThread.looper, UiAutomationConnection())
        uiAutomation.connect()
        startMonitor()
        startTimestamp = System.currentTimeMillis()
    }

    private var lastHandledNodeInfo: AccessibilityNodeInfo? = null
    private var lastHandleTimestamp = 0L

    private fun startMonitor() {
        uiAutomation.serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED //flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        uiAutomation.setOnAccessibilityEventListener { event ->
            if (event.packageName == null) {
                return@setOnAccessibilityEventListener
            }
            //filter out system apps
            if (event.packageName.startsWith("com.android")) {
                return@setOnAccessibilityEventListener
            }
            val windowInfo =
                uiAutomation.rootInActiveWindow ?: return@setOnAccessibilityEventListener
            windowInfo.findAccessibilityNodeInfosByText("跳过")?.forEach { node ->
                Log.i(TAG, "duration: ${System.currentTimeMillis() - lastHandleTimestamp}")
                Log.i(TAG, "last: $lastHandledNodeInfo cur: $node, equal?: ${Objects.equals(lastHandledNodeInfo, node)}")
                //distinct nodes within 500 milliseconds
                if (System.currentTimeMillis() - lastHandleTimestamp < 500 && Objects.equals(lastHandledNodeInfo, node)) {
                    return@forEach
                }
                val text = node.text
                if (text?.contains(Regex("\\d")) == true) {
                    if (node.isClickable) {
                        Log.i(TAG, node.toString())
                        Log.i(TAG, "Skipped!")
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } else { //fallback
                        Log.i(TAG, node.toString())
                        Log.i(TAG, "Fallback!")
                        val downTime = SystemClock.uptimeMillis()
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        val downAction =
                            MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, rect.exactCenterX(), rect.exactCenterY(), 0)
                        downAction.source = InputDevice.SOURCE_TOUCHSCREEN
                        uiAutomation.injectInputEvent(downAction, true)
                        val upAction =
                            MotionEvent.obtain(downAction).apply { action = MotionEvent.ACTION_UP }
                        uiAutomation.injectInputEvent(upAction, true)
                        upAction.recycle()
                        downAction.recycle()
                    }
                    lastHandledNodeInfo = node
                    lastHandleTimestamp = System.currentTimeMillis()
                }
            }
        }
    }

    override fun disconnect() {
        check(handlerThread.isAlive) { "Already disconnected!" }
        try {
            //Calling mUiAutomation.disconnect() will cause an error, because our AccessibilityServiceClient
            //is injected without calling init(), then we just manually unregister the client via reflection.
            uiAutomation.javaClass.getDeclaredField("mUiAutomationConnection").apply {
                isAccessible = true
            }.get(uiAutomation).run {
                javaClass.getDeclaredMethod("disconnect").apply { isAccessible = true }.invoke(this)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        } finally {
            handlerThread.quit()
        }
    }

    override fun sayHello() = "Hello from remote service! My uid is ${Os.geteuid()} & my pid is ${Os.getpid()}"

    override fun isConnnected() = handlerThread.isAlive

    override fun getStartTimestamp() = startTimestamp

    override fun setConfig(config: AutomatorConfig?) {
        if (config != null) {
            this.config = config
        }
    }

    override fun destroy() {
        Log.i(TAG, "Goodbye, world!")
        disconnect()
        exitProcess(0)
    }
}