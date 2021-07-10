package top.xjunz.library.automator

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import `$android`.hardware.input.InputManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.IPackageManager
import android.graphics.Rect
import android.os.*
import android.system.Os
import android.util.Log
import android.util.LruCache
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import rikka.shizuku.SystemServiceHelper
import java.io.*
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess


/**
 * @author xjunz 2021/6/22 23:01
 */
class AutomatorConnection : IAutomatorConnection.Stub() {

    companion object {
        private const val HANDLER_THREAD_NAME = "AutomatorHandlerThread"
        const val TAG = "automator"
    }

    private var config: AutomatorConfig = AutomatorConfig()
    private var maxRecordCount = 500
    private val handlerThread = HandlerThread(HANDLER_THREAD_NAME)
    private var startTimestamp = -1L
    private lateinit var uiAutomation: UiAutomation
    private var skippedTimes = 0
    private val inputManager by lazy {
        InputManager.getInstance()
    }
    private lateinit var timesFileDescriptor: ParcelFileDescriptor
    private var onSkipListener: OnSkipListener? = null
    private val recordQueue by lazy {
        LinkedList<String>()
    }

    override fun connect() {
        check(!handlerThread.isAlive) { "Already connected!" }
        try {
            handlerThread.start()
            uiAutomation = UiAutomation(handlerThread.looper, UiAutomationConnection())
            uiAutomation.connect()
            log("Launcher package name is $launcherName")
            startMonitoring()
            startTimestamp = System.currentTimeMillis()
        } catch (t: Throwable) {
            exitProcess(0)
        }
    }

    private var previousNode: AccessibilityNodeInfo? = null
    private var previousTimestamp = 0L

    private val launcherName by lazy {
        IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package"))
            .getHomeActivities(arrayListOf())?.packageName
    }

    private fun notifySkipped(type: Int, node: AccessibilityNodeInfo) {
        skippedTimes++
        onSkipListener?.onSkip(skippedTimes, type, node)
        log("Skipped: type=$type")
    }

    private fun logNoRecord(msg: String) {
        Log.i(TAG, msg)
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
        recordQueue.add(msg)
        if (recordQueue.size > maxRecordCount) {
            recordQueue.removeFirst()
        }
    }

    private fun startMonitoring() {
        uiAutomation.serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED //or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS //or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        uiAutomation.setOnAccessibilityEventListener { event ->
            val source = event.source ?: return@setOnAccessibilityEventListener
            val packageName = source.packageName ?: return@setOnAccessibilityEventListener
            if (launcherName == packageName) {
                logNoRecord("ignore launcher")
                return@setOnAccessibilityEventListener
            }
            source.findAccessibilityNodeInfosByText("跳过")?.forEach { node ->
                val text = node.text ?: return@setOnAccessibilityEventListener
                if (System.currentTimeMillis() - previousTimestamp < 1000 && Objects.equals(previousNode, node)) {
                    logNoRecord("duplicated within one second")
                    return@setOnAccessibilityEventListener
                }

                log("-----Possible Node Found (${System.currentTimeMillis()})-----\n$node")
                previousNode = node
                previousTimestamp = System.currentTimeMillis()
                if (text.length > 6) {
                    log("Filtered: text too long")
                    return@setOnAccessibilityEventListener
                }
                val windowInfo = uiAutomation.rootInActiveWindow ?: return@setOnAccessibilityEventListener
                val windowRect = Rect()
                windowInfo.getBoundsInScreen(windowRect)
                val nodeRect = Rect()
                node.getBoundsInScreen(nodeRect)
                if (config.checkDigit && !checkDigit(node.text)) {
                    log("Filtered: without any digit")
                    return@setOnAccessibilityEventListener
                }
                if (config.checkSize && !checkSize(windowRect, nodeRect)) {
                    log("Filtered: illegally sized")
                    return@setOnAccessibilityEventListener
                }
                if (config.checkRegion && !checkRegion(nodeRect, windowRect)) {
                    log("Filtered: illegally located")
                    return@setOnAccessibilityEventListener
                }
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    notifySkipped(TYPE_DIRECT, node)
                } else {
                    node.parent?.run {
                        if (isClickable) {
                            val parentRect = Rect()
                            getBoundsInScreen(parentRect)
                            if (!checkSize(windowRect, parentRect)) {
                                log("Filtered: parent illegal sized ($parentRect)")
                                return@setOnAccessibilityEventListener
                            }
                            performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            notifySkipped(TYPE_PARENT, node)
                            return@setOnAccessibilityEventListener
                        }
                        log("Filtered: unclickable node and parent")
                    }
                }
                /* if (config.fallbackInjectingEvents) {
                     //fallback, this normally would not happen
                     log("Fallback!")
                     val downTime = SystemClock.uptimeMillis()
                     val rect = Rect()
                     node.getBoundsInScreen(rect)
                     val downAction = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN,
                         rect.exactCenterX(), rect.exactCenterY(), 0)
                     downAction.source = InputDevice.SOURCE_TOUCHSCREEN
                     inputManager.injectInputEvent(downAction, 2)
                     val upAction =
                         MotionEvent.obtain(downAction).apply { action = MotionEvent.ACTION_UP }
                     inputManager.injectInputEvent(upAction, 2)
                     upAction.recycle()
                     downAction.recycle()
                     notifySkipped(TYPE_INJECT, node)
                 }*/
            }
        }
    }

    private fun checkRegion(nodeRect: Rect, windowRect: Rect): Boolean {
        if (nodeRect.exactCenterX() > windowRect.width() / 4f && nodeRect.exactCenterX() < windowRect.width() / 4f * 3) {
            log(">>out of x range<<")
            return false
        }
        if (nodeRect.exactCenterY() > windowRect.height() / 4f && nodeRect.exactCenterY() < windowRect.height() / 4f * 3) {
            log(">>out of y range<<")
            return false
        }
        return true
    }

    private fun checkDigit(text: CharSequence?): Boolean {
        text?.forEach {
            if (it in '0'..'9') {
                return true
            }
        }
        return false
    }

    private fun checkSize(windowRect: Rect, nodeRect: Rect): Boolean {
        val nw = nodeRect.width().coerceAtLeast(nodeRect.height())
        val nh = nodeRect.width().coerceAtMost(nodeRect.height())
        val isLandscape = windowRect.width() > windowRect.height()
        if (isLandscape) {
            if (nw > windowRect.width() / 6) {
                log(">>over wide<<")
                return false
            }
            if (nh > windowRect.height() / 4) {
                log(">>over high<<")
                return false
            }
        } else {
            if (nw >= windowRect.width() / 3) {
                log(">>over wide<<")
                return false
            }
            if (nh >= windowRect.height() / 8) {
                log(">>over high<<")
                return false
            }
        }
        return true
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
            handlerThread.quitSafely()
        }
    }

    override fun sayHello() = "Hello from remote service! My uid is ${Os.geteuid()} & my pid is ${Os.getpid()}"

    override fun isConnected() = handlerThread.isAlive

    override fun getStartTimestamp() = startTimestamp

    override fun setConfig(config: AutomatorConfig?) {
        if (config != null) {
            this.config = config
        }
    }

    override fun getPid() = Os.getpid()

    override fun getSkippedTimes() = skippedTimes

    override fun setFileDescriptors(pfd: ParcelFileDescriptor?) {
        timesFileDescriptor = checkNotNull(pfd) {
            "Null ParcelFileDescriptor"
        }
        FileInputStream(timesFileDescriptor.fileDescriptor).bufferedReader().useLines { s ->
            s.forEach {
                skippedTimes = it.toIntOrNull() ?: 0
                return@useLines
            }
        }
    }

    override fun setOnSkipListener(listener: OnSkipListener?) {
        onSkipListener = listener
    }

    override fun removeOnSkipListener() {
        onSkipListener = null
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            FileOutputStream(timesFileDescriptor.fileDescriptor).bufferedWriter().use { writer ->
                recordQueue.forEach {
                    writer.write(it)
                    writer.newLine()
                }
            }
        })
    }

    override fun destroy() {
        log("Goodbye, world! (${System.currentTimeMillis()})")
        disconnect()
        exitProcess(0)
    }
}