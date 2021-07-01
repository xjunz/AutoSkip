package top.xjunz.library.automator

import `$android`.app.UiAutomation
import `$android`.app.UiAutomationConnection
import `$android`.hardware.input.InputManager
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.lang.Process
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

    private var lastHandledNodeInfo: AccessibilityNodeInfo? = null
    private var lastHandleTimestamp = 0L

    private fun startMonitoring() {
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
            val windowInfo = uiAutomation.rootInActiveWindow ?: return@setOnAccessibilityEventListener
            windowInfo.findAccessibilityNodeInfosByText("跳过")?.forEach { node ->
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
                        skippedTimes++
                    } else {
                        /*node.parent?.run {
                            if(isClickable){
                                performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            }
                        }?: */run {
                            //fallback
                            Log.i(TAG, node.toString())
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
                Log.i(TAG, it)
            }
        }
        FileOutputStream(pfd.fileDescriptor).also { outputStream = it }
        val bmap = uiAutomation.takeScreenshot()
        bmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
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