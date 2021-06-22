package top.xjunz.library.automator.impl

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.HandlerThread
import android.view.accessibility.AccessibilityEvent
import top.xjunz.library.automator.IAutomatorConnection
import top.xjunz.library.automator.IOnAccessibilityEventListener
import kotlin.system.exitProcess


/**
 * @author xjunz 2021/6/22 23:01
 */
class AutomatorConnection : IAutomatorConnection.Stub() {
    companion object {
        private const val HANDLER_THREAD_NAME = "UiAutomatorHandlerThread"
    }

    private val mHandlerThread = HandlerThread(HANDLER_THREAD_NAME)

    private lateinit var mUiAutomation: UiAutomation

    override fun connect() {
        check(!mHandlerThread.isAlive) { "Already connected!" }
        mHandlerThread.start()
        mUiAutomation = UiAutomation(mHandlerThread.looper, UiAutomationConnection())
        mUiAutomation.connect()
    }

    override fun disconnect() {
        check(mHandlerThread.isAlive) { "Already disconnected!" }
        mUiAutomation.disconnect()
        mHandlerThread.quit()
    }

    override fun takeScreenshot(crop: Rect?, rotation: Int): Bitmap {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        exitProcess(0)
    }

    override fun setOnAccessibilityEventListener(client: IOnAccessibilityEventListener?) {
        mUiAutomation.setOnAccessibilityEventListener { event -> client!!.onAccessibilityEvent(event) }
    }

    fun setCompressedLayoutHierarchy(compressed: Boolean) {
        val info = mUiAutomation.serviceInfo
        if (compressed) info.flags = info.flags and AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS.inv() else info.flags = info.flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        mUiAutomation.serviceInfo = info
    }
}