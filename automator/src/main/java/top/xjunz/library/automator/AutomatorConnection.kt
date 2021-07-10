package top.xjunz.library.automator

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.IPackageManager
import android.graphics.Rect
import android.os.*
import android.system.Os
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import rikka.shizuku.SystemServiceHelper
import java.io.*
import java.util.*
import kotlin.system.exitProcess


/**
 * @author xjunz 2021/6/22 23:01
 */
class AutomatorConnection : IAutomatorConnection.Stub() {

    companion object {
        private const val HANDLER_THREAD_NAME = "AutomatorHandlerThread"
        private const val APPLICATION_ID = "top.xjunz.automator"
        const val TAG = "automator"
        const val TYPE_DIRECT = 1;
        const val TYPE_PARENT = 2;
    }

    private lateinit var uiAutomation: UiAutomation
    private var config: AutomatorConfig = AutomatorConfig()
    private var maxRecordCount = 500
    private val handlerThread = HandlerThread(HANDLER_THREAD_NAME)
    private var startTimestamp = -1L
    private var skippedTimes = 0
    private lateinit var timesFileDescriptor: ParcelFileDescriptor
    private var onSkipListener: OnSkipListener? = null
    private val recordQueue = LinkedList<String>()
    private var isPaused = false

    override fun connect() {
        check(!handlerThread.isAlive) { "Already connected!" }
        try {
            handlerThread.start()
            uiAutomation = UiAutomation(handlerThread.looper, UiAutomationConnection())
            uiAutomation.connect()
            logAndRecord("Launcher package name is $launcherName")
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
        logAndRecord("Skipped: type=$type")
    }

    private fun log(msg: String) {
        Log.i(TAG, msg)
    }

    private fun logAndRecord(msg: String) {
        if (!isPaused) {
            Log.i(TAG, msg)
            recordQueue.add(msg)
            if (recordQueue.size > maxRecordCount) {
                recordQueue.removeFirst()
            }
        }
    }

    private fun startMonitoring() {
        uiAutomation.serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED //or AccessibilityEvent.TYPE_WINDOWS_CHANGED
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS //or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        uiAutomation.setOnAccessibilityEventListener listener@{ event ->
            try {
                if (isPaused) return@listener
                val source = event.source ?: return@listener
                val packageName = source.packageName ?: return@listener
                if (launcherName == packageName) return@listener
                if (packageName.startsWith("com.android")) return@listener
                if (APPLICATION_ID == packageName) return@listener
                source.findAccessibilityNodeInfosByText("跳过").forEach { node ->
                    if (System.currentTimeMillis() - previousTimestamp < 1000 && Objects.equals(previousNode, node)) {
                        log("duplicated within one second")
                        return@listener
                    }
                    previousNode = node
                    previousTimestamp = System.currentTimeMillis()
                    logAndRecord("-----Possible Node Found-----\n$node")
                    if (!checkText(node.text)) {
                        logAndRecord("Filtered: illegal text")
                        return@listener
                    }
                    val windowRect = Rect()
                    uiAutomation.rootInActiveWindow.getBoundsInScreen(windowRect)
                    val nodeRect = Rect()
                    node.getBoundsInScreen(nodeRect)
                    if (!checkSize(windowRect, nodeRect)) {
                        logAndRecord("Filtered: illegally sized")
                        return@listener
                    }
                    if (!checkRegion(nodeRect, windowRect)) {
                        logAndRecord("Filtered: illegally located")
                        return@listener
                    }
                    if (node.isClickable) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        notifySkipped(TYPE_DIRECT, node)
                        return@listener
                    } else {
                        node.parent.run {
                            if (isClickable) {
                                val parentRect = Rect()
                                getBoundsInScreen(parentRect)
                                if (!checkSize(windowRect, parentRect)) {
                                    logAndRecord("Filtered: parent illegal sized ($parentRect)")
                                    return@listener
                                }
                                performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                notifySkipped(TYPE_PARENT, node)
                                return@listener
                            }
                        }
                        logAndRecord("Filtered: unreachable")
                        /* fallback, this normally would not happen
                         if (config.fallbackInjectingEvents) {
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
            } catch (t: Throwable) {
                t.printStackTrace()
            } finally {
                event.recycle()
            }
        }
    }

    private fun checkRegion(nodeRect: Rect, windowRect: Rect): Boolean {
        if (nodeRect.exactCenterX() > windowRect.width() / 4f && nodeRect.exactCenterX() < windowRect.width() / 4f * 3) {
            logAndRecord(">> out of x range <<")
            return false
        }
        if (nodeRect.exactCenterY() > windowRect.height() / 4f && nodeRect.exactCenterY() < windowRect.height() / 4f * 3) {
            logAndRecord(">> out of y range <<")
            return false
        }
        return true
    }

    private fun checkText(text: CharSequence): Boolean {
        if (text.length > 6) {
            logAndRecord(">> text over-length <<")
            return false
        }
        var count = 0
        text.forEach {
            if (it > '~') {
                count++
            }
        }
        if (count > 3) {
            logAndRecord(">> too many non-ascii digits <<")
            return false
        }
        return true
    }

    private fun checkSize(windowRect: Rect, nodeRect: Rect): Boolean {
        val nw = nodeRect.width().coerceAtLeast(nodeRect.height())
        val nh = nodeRect.width().coerceAtMost(nodeRect.height())
        val isLandscape = windowRect.width() > windowRect.height()
        if (isLandscape) {
            if (nw > windowRect.width() / 6) {
                logAndRecord(">> landscape: over wide <<")
                return false
            }
            if (nh > windowRect.height() / 4) {
                logAndRecord(">> landscape: over high <<")
                return false
            }
        } else {
            if (nw >= windowRect.width() / 3) {
                logAndRecord(">> portrait: over wide <<")
                return false
            }
            if (nh >= windowRect.height() / 8) {
                logAndRecord(">> portrait: over high <<")
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

    /**
     * Check whether a source node contains any legal target to skip.
     *
     * @return the [Result] of this check or null if the check procedure is not normally completed
     */
    override fun testCheck(): Result? {
        try {
            val windowNode = uiAutomation.rootInActiveWindow
            windowNode.findAccessibilityNodeInfosByText("跳过").forEach { node ->
                if (!checkText(node.text)) {
                    return Result(false, Result.REASON_ILLEGAL_TEXT, node)
                }
                val windowRect = Rect()
                windowNode.getBoundsInScreen(windowRect)
                val nodeRect = Rect()
                node.getBoundsInScreen(nodeRect)
                if (!checkSize(windowRect, nodeRect)) {
                    return Result(false, Result.REASON_ILLEGAL_LOCATION, node)
                }
                if (!checkRegion(nodeRect, windowRect)) {
                    return Result(false, Result.REASON_ILLEGAL_LOCATION, node)
                }
                if (node.isClickable) {
                    return Result(true, Result.REASON_MASK_CHILD, node)
                } else {
                    node.parent.run {
                        if (isClickable) {
                            val parentRect = Rect()
                            getBoundsInScreen(parentRect)
                            if (!checkSize(windowRect, parentRect)) {
                                return Result(false, Result.REASON_MASK_PARENT
                                        or Result.REASON_ILLEGAL_SIZE, node)
                            }
                            return Result(true, Result.REASON_MASK_PARENT, node)
                        }
                    }
                    return Result(false, Result.REASON_MASK_PARENT or Result.REASON_NOT_CLICKABLE, node)
                }
            }
            return null
        } catch (t: Throwable) {
            t.printStackTrace()
            return null
        }
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
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
        logAndRecord("Goodbye, world! (${System.currentTimeMillis()})")
        disconnect()
        exitProcess(0)
    }
}