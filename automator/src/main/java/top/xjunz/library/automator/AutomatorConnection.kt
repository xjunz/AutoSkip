package top.xjunz.library.automator

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import `$android`.hardware.input.InputManager
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.os.*
import android.system.Os
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.*
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

    private var config: AutomatorConfig = AutomatorConfig()

    private val handlerThread = HandlerThread(HANDLER_THREAD_NAME)
    private var startTimestamp = -1L
    private lateinit var uiAutomation: UiAutomation
    private var skippedTimes = 0
    private var filePath: String? = null
    private lateinit var outputStream: OutputStream
    private val inputManager by lazy {
        InputManager.getInstance()
    }

    init {
        //When the VM terminates, let's do some persisting work
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutdown hook: VM exits.")
        })
    }

    override fun connect() {
        check(!handlerThread.isAlive) { "Already connected!" }
        try {
            handlerThread.start()
            uiAutomation = UiAutomation(handlerThread.looper, UiAutomationConnection())
            uiAutomation.connect()
            startMonitoring()
            startTimestamp = System.currentTimeMillis()
        } catch (t: Throwable) {
            t.printStackTrace(PrintStream(outputStream))
            exitProcess(0)
        }
    }

    private var previousNode: AccessibilityNodeInfo? = null
    private var previousTimestamp = 0L

    private fun logFilteredNode(node: AccessibilityNodeInfo, reason: String) {
        val region = Rect()
        node.getBoundsInScreen(region)
        Log.i(TAG, "Filtered out node: ${node.packageName}:${node.className.toString().substringAfterLast('.')}:${node.text}, $region" +
                "\ndue to $reason")
    }

    private fun containsNumberNoRegex(cs: CharSequence): Boolean {
        cs.asIterable().forEach {
            if (it in '0'..'9') {
                return true
            }
        }
        return false
    }

    private fun startMonitoring() {
        uiAutomation.serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED //flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        uiAutomation.setOnAccessibilityEventListener { event ->
            val packageName = event.packageName ?: return@setOnAccessibilityEventListener
            //filter out system apps
            if (packageName.startsWith("com.android")) {
                return@setOnAccessibilityEventListener
            }
            val windowInfo = uiAutomation.rootInActiveWindow ?: return@setOnAccessibilityEventListener
            //strict filter: child count
            Log.i(TAG, "Child count: ${windowInfo.childCount}")
            windowInfo.findAccessibilityNodeInfosByText("跳过")?.forEach { node ->
                //distinct nodes within 500 milliseconds
                if (System.currentTimeMillis() - previousTimestamp < 500 && Objects.equals(previousNode, node)) {
                    return@setOnAccessibilityEventListener
                }
                previousNode = node
                previousTimestamp = System.currentTimeMillis()
                val windowRect = Rect()
                windowInfo.getBoundsInScreen(windowRect)
                val nodeRect = Rect()
                node.getBoundsInScreen(nodeRect)
                if (config.checkSize) {
                    val nw = nodeRect.width().coerceAtMost(nodeRect.height())
                    val nh = nodeRect.width().coerceAtLeast(nodeRect.height())
                    val ww = windowRect.width().coerceAtMost(windowRect.height())
                    val wh = windowRect.width().coerceAtLeast(windowRect.height())
                    if (nw >= ww / 2) {
                        logFilteredNode(node, "width too large")
                        return@setOnAccessibilityEventListener
                    }
                    if (nh >= wh / 4) {
                        logFilteredNode(node, "height too large")
                        return@setOnAccessibilityEventListener
                    }
                }
                if (config.checkRegion) {
                    if (nodeRect.exactCenterX() > windowRect.width() / 4f && nodeRect.exactCenterX() < windowRect.width() / 4f * 3) {
                        logFilteredNode(node, "out of x range")
                        return@setOnAccessibilityEventListener
                    }
                    if (nodeRect.exactCenterY() > windowRect.height() / 4f && nodeRect.exactCenterY() < windowRect.height() / 4f * 3) {
                        logFilteredNode(node, "out of y range")
                        return@setOnAccessibilityEventListener
                    }
                }
                if (config.checkDigit) {
                    if (!containsNumberNoRegex(node.text)) {
                        logFilteredNode(node, "no digit")
                        return@setOnAccessibilityEventListener
                    }
                }
                if (node.isClickable) {
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    skippedTimes++
                } else {
                    node.parent?.run {
                        if (isClickable) {
                            val parentRect = Rect()
                            if (parentRect.width() > 2 * nodeRect.width() || parentRect.height() > 2 * nodeRect.height()) {
                                logFilteredNode(node, "parent too large")
                                return@run
                            }
                            performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return@setOnAccessibilityEventListener
                        }
                        logFilteredNode(node, "parent not clickable")
                    }
                    if (config.fallbackInjectingEvents) {
                        //fallback
                        Log.i(TAG, "Fallback!")
                        val downTime = SystemClock.uptimeMillis()
                        val rect = Rect()
                        node.getBoundsInScreen(rect)
                        val downAction = MotionEvent.obtain(
                            downTime,
                            downTime,
                            MotionEvent.ACTION_DOWN,
                            rect.exactCenterX(),
                            rect.exactCenterY(),
                            0)
                        downAction.source = InputDevice.SOURCE_TOUCHSCREEN
                        inputManager.injectInputEvent(downAction, 2)
                        val upAction =
                            MotionEvent.obtain(downAction).apply { action = MotionEvent.ACTION_UP }
                        inputManager.injectInputEvent(upAction, 2)
                        upAction.recycle()
                        downAction.recycle()
                        skippedTimes++
                    }
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
            handlerThread.quitSafely()
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

    override fun getPid() = Os.getpid()

    override fun getSkippedTimes() = skippedTimes

    override fun setFileDescriptors(pfd: ParcelFileDescriptor?) {
        FileInputStream(pfd!!.fileDescriptor).bufferedReader().useLines { s ->
            s.forEach {
                skippedTimes = it.toIntOrNull() ?: 0
            }
        }
        outputStream = FileOutputStream(pfd.fileDescriptor)
    }

    fun setFilePath(path: String?) {
        filePath = path
        path?.let {
            FileInputStream(it).bufferedReader().useLines { s ->
                s.forEach { line ->
                    skippedTimes = line.toIntOrNull() ?: 0
                    return@useLines
                }
            }
        }
    }

    override fun destroy() {
        Log.i(TAG, "Goodbye, world!")
        disconnect()
        exitProcess(0)
    }
}