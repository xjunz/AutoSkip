package top.xjunz.automator

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.app.UiAutomationConnection
import android.app.UiAutomationHidden
import android.content.pm.IPackageManager
import android.graphics.Rect
import android.os.*
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants.SEEK_SET
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.rikka.tools.refine.Refine
import rikka.shizuku.SystemServiceHelper
import top.xjunz.automator.model.Result
import top.xjunz.automator.util.Records
import top.xjunz.automator.util.formatCurrentTime
import java.io.*
import java.util.*
import kotlin.system.exitProcess


/**
 * The implementation of [IAutomatorConnection]. This class is expected to be executed in a privileged
 * process, e.g. shell and su, to perform functions normally.
 *
 * @see IAutomatorConnection
 *
 * @author xjunz 2021/6/22 23:01
 */
class AutomatorConnection : IAutomatorConnection.Stub() {

    companion object {
        private const val APPLICATION_ID = "top.xjunz.automator"
        const val TAG = "automator"
        const val MAX_RECORD_COUNT: Short = 500
        val SKIP_KEYWORD:Array<String> = arrayOf("跳过", "skip")
    }

    private lateinit var uiAutomationHidden: UiAutomationHidden
    private val uiAutomation by lazy {
        Refine.unsafeCast<UiAutomation>(uiAutomationHidden)
    }
    private val handlerThread = HandlerThread("AutomatorHandlerThread")
    private val handler by lazy {
        Handler(handlerThread.looper)
    }
    private var serviceStartTimestamp = -1L
    private var skippingCount = -1
    private var countFileDescriptor: ParcelFileDescriptor? = null
    private var logFileDescriptor: ParcelFileDescriptor? = null
    private var recordFileDescriptor: ParcelFileDescriptor? = null
    private val recordQueue = LinkedList<String>()
    private var firstCheckRecordIndex = -1
    private val records by lazy {
        Records(recordFileDescriptor!!.fileDescriptor)
    }
    private var monitoring: Boolean = false

    init {
        try {
            log("========Start Connecting========")
            log(sayHello())
            handlerThread.start()
            uiAutomationHidden = UiAutomationHidden(handlerThread.looper, UiAutomationConnection())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                uiAutomationHidden.connect(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
            } else {
                log("Marshmallow don't support FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES")
                uiAutomationHidden.connect()
            }
            log("The UiAutomation is connected at ${formatCurrentTime()}")
        } catch (t: Throwable) {
            dumpError(t)
            exitProcess(0)
        }
    }

    private val launcherName by lazy {
        IPackageManager.Stub.asInterface(SystemServiceHelper.getSystemService("package"))
            ?.getHomeActivities(arrayListOf())?.packageName
    }

    private fun log(any: Any?, queued: Boolean = true) {
        (any?.toString() ?: "null").let {
            Log.i(TAG, it)
            if (queued) {
                recordQueue.add(it)
                if (recordQueue.size > MAX_RECORD_COUNT) {
                    if (firstCheckRecordIndex >= 0) {
                        //truncate check result records
                        recordQueue.removeAt(firstCheckRecordIndex)
                    } else {
                        recordQueue.removeFirst()
                    }
                }
            }
        }
    }

    private fun dumpResult(result: Result, queued: Boolean) {
        if (firstCheckRecordIndex == -1) firstCheckRecordIndex = recordQueue.size
        val sb = StringBuilder()
        sb.append("========Check Result========")
            .append("\ntimestamp: ${formatCurrentTime()}")
            .append("\nresult: $result")
        if (result.passed) {
            val injectType = if (result.getInjectionType() == Result.INJECTION_ACTION) "action" else "event"
            sb.append("\nskip: count=$skippingCount, injection type=$injectType")
        }
        log(sb.toString(), queued)
    }

    private fun dumpError(t: Throwable) {
        log("========Error Occurred========")
        log(t.stackTraceToString())
    }

    override fun startMonitoring() {
        var distinct = false
        var oldPkgName: String? = null
        uiAutomation.serviceInfo = uiAutomation.serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOWS_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        checkResult = Result()
        uiAutomation.setOnAccessibilityEventListener listener@{ event ->
            try {
                val packageName = event.packageName?.toString() ?: return@listener
                if (oldPkgName != packageName) distinct = true
                oldPkgName = packageName
                val source = event.source ?: return@listener
                //ignore the launcher app
                if (packageName == launcherName) return@listener
                //ignore the android framework
                if (packageName == "android") return@listener
                //ignore android build-in apps
                if (packageName.startsWith("com.android")) return@listener
                //ignore the host app
                if (packageName == APPLICATION_ID) return@listener
                //start checking
                checkSource(source, checkResult.apply { reset() }, true)
                //to avoid repeated increments, increment only when distinct
                if (checkResult.passed && distinct) {
                    skippingCount++
                    distinct = false
                    records.putResult(checkResult)
                }
                //should dump result after incrementing skipping count, cuz we need to
                //dump the latest count
                if (checkResult.getReason() != Result.REASON_ILLEGAL_TARGET) {
                    //dump only when the result is distinct
                    if (lastResultHash != checkResult.hashCode()) {
                        dumpResult(checkResult, true)
                        lastResultHash = checkResult.hashCode()
                    }
                }
            } catch (t: Throwable) {
                checkResult.maskReason(Result.REASON_ERROR)
                dumpError(t)
            } finally {
                event.recycle()
            }
        }
        serviceStartTimestamp = System.currentTimeMillis()
        monitoring = true
        log("The monitoring is started at ${formatCurrentTime()}")
    }

    override fun isMonitoring() = monitoring

    override fun sayHello() = "Hello from the remote service! My uid is ${Os.geteuid()} & my pid is ${Os.getpid()}"

    override fun getStartTimestamp() = serviceStartTimestamp

    override fun getPid() = Os.getpid()

    override fun getSkippingCount() = skippingCount

    override fun setFileDescriptors(pfds: Array<ParcelFileDescriptor>?) {
        check(pfds != null && pfds.size == 3)
        countFileDescriptor = pfds[0]
        checkNotNull(countFileDescriptor)
        logFileDescriptor = pfds[1]
        checkNotNull(logFileDescriptor)
        recordFileDescriptor = pfds[2]
        checkNotNull(recordFileDescriptor)
        log("File descriptors received")
        try {
            FileInputStream(countFileDescriptor!!.fileDescriptor).bufferedReader().useLines {
                skippingCount = it.firstOrNull()?.toIntOrNull() ?: 0
                log("The skipping count parsed: $skippingCount")
            }
            records.parse().getRecordCount().also {
                if (skippingCount != it) {
                    skippingCount = it
                    log("Skipping count inconsistency detected! Reassign the skipping count to $it")
                }
            }
            log("The record file parsed")
        } catch (t: Throwable) {
            if (t is Records.ParseException) {
                log(t.message)
            } else {
                dumpError(t)
            }
        }
    }

    override fun setBasicEnvInfo(info: String?) = log(info)

    override fun setSkippingCount(count: Int) {
        check(count > -1)
        skippingCount = count
    }

    /**
     * A result instance for monitoring to avoid frequent object allocations.
     */
    private lateinit var checkResult: Result

    /**
     * The hashcode record of the last check [Result].
     */
    private var lastResultHash = -1

    /**
     * Launch a standalone check.
     *
     * @param listener a listener to be notified the result
     */
    override fun standaloneCheck(listener: OnCheckResultListener) {
        handler.post {
            val standaloneResult = Result()
            try {
                val possibleAccessibilityNodeInfo:MutableList<AccessibilityNodeInfo> = mutableListOf()
                SKIP_KEYWORD.forEach {
                    possibleAccessibilityNodeInfo.addAll(uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText(it))
                }
                for(it in possibleAccessibilityNodeInfo) {
                    // skip the EditText
                    if(it.isEditable) continue
                    checkSource(it, standaloneResult.apply { reset() }, false)
                }
            } catch (t: Throwable) {
                dumpError(t)
                standaloneResult.maskReason(Result.REASON_ERROR)
            } finally {
                //dump the result before calling the listener, cuz a marshall of result would
                //recycle the node, hence, we could not dump it any more.
                dumpResult(standaloneResult, !standaloneResult.passed)
                listener.onCheckResult(standaloneResult)
            }
        }
    }

    override fun getRecords() = records.asList()

    /**
     * Inject a mock finger click event via [UiAutomation.injectInputEvent] into a specific [rect],
     * corresponding to [Result.INJECTION_EVENT].
     */
    private fun injectFingerClickEvent(rect: Rect) {
        val downTime = SystemClock.uptimeMillis()
        val downAction = MotionEvent.obtain(
            downTime, downTime, MotionEvent.ACTION_DOWN,
            rect.exactCenterX(), rect.exactCenterY(), 0
        )
        downAction.source = InputDevice.SOURCE_TOUCHSCREEN
        uiAutomation.injectInputEvent(downAction, true)
        val upAction = MotionEvent.obtain(downAction).apply { action = MotionEvent.ACTION_UP }
        uiAutomation.injectInputEvent(upAction, true)
        upAction.recycle()
        downAction.recycle()
    }

    /**
     * Launch a standalone check finding whether a [source] node contains but one single legal target
     * to be skipped.
     *
     * @param source the source node used to find the possible target
     * @param result the result of this check
     * @param inject should inject click to the detected legal target or not
     */
    private fun checkSource(source: AccessibilityNodeInfo, result: Result, inject: Boolean) {
        result.pkgName = source.packageName.toString()
        val possibleAccessibilityNodeInfo:MutableList<AccessibilityNodeInfo> = mutableListOf()
        SKIP_KEYWORD.forEach {
            possibleAccessibilityNodeInfo.addAll(source.findAccessibilityNodeInfosByText(it))
        }
        possibleAccessibilityNodeInfo.run {
            when (size) {
                0 -> result.maskReason(Result.REASON_ILLEGAL_TARGET or Result.REASON_MASK_PORTRAIT)
                1 -> checkNode(first(), result, inject)
                else -> result.maskReason(Result.REASON_ILLEGAL_TARGET or Result.REASON_MASK_TRANSVERSE)
            }
        }
    }

    private fun checkNode(node: AccessibilityNodeInfo, result: Result, inject: Boolean) {
        result.nodeHash = node.hashCode()
        if (!node.isVisibleToUser) {
            result.maskReason(Result.REASON_INVISIBLE)
            return
        }
        if (node.isEditable){
            result.maskReason(Result.REASON_EDITABLE)
            return
        }
        if (!checkText(node.text, result)) return
        val nodeRect = Rect().also { node.getBoundsInScreen(it) }
        result.bounds = nodeRect
        val windowRect = Rect().also { node.window.getBoundsInScreen(it) }
        if (!checkRegion(nodeRect, windowRect, result)) return
        if (!checkSize(nodeRect, windowRect, result)) return
        //it's enough strict to confirm a target after all these checks, so we consider any node
        //reaching here as passed. Node click-ability is not a sufficient criteria for check.
        result.passed = true
        if (node.isClickable) {
            if (inject) node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            result.maskReason(Result.REASON_MASK_NOT_CLICKABLE)
            node.parent?.run {
                val parentBounds = Rect().also { getBoundsInScreen(it) }
                result.parentBounds = parentBounds
                if (isClickable && checkSize(parentBounds, windowRect, result)) {
                    result.bounds = parentBounds
                    if (inject) performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return
                }
            }
            //the parent has its fault when reaches this step
            result.maskReason(Result.REASON_MASK_PARENT)
            if (inject) injectFingerClickEvent(nodeRect)
        }
    }

    private fun checkText(text: CharSequence, result: Result): Boolean {
        result.text = text.trim().toString()
        if (text.length > 10) {
            result.maskReason(Result.REASON_ILLEGAL_TEXT or Result.REASON_MASK_TRANSVERSE)
            return false
        }
        if (text.filter { it > '~' }.count() > 4) {
            result.maskReason(Result.REASON_ILLEGAL_TEXT or Result.REASON_MASK_PORTRAIT)
            return false
        }
        return true
    }

    private fun checkRegion(nodeRect: Rect, windowRect: Rect, result: Result): Boolean {
        if (/*nodeRect.exactCenterX() > windowRect.width() / 4f &&*/ nodeRect.exactCenterX() < windowRect.width() / 3f * 2) {
            result.maskReason(Result.REASON_ILLEGAL_LOCATION or Result.REASON_MASK_TRANSVERSE)
            return false
        }
//        if (nodeRect.exactCenterY() > windowRect.height() / 4f && nodeRect.exactCenterY() < windowRect.height() / 3f * 2) {
//            result.maskReason(Result.REASON_ILLEGAL_LOCATION or Result.REASON_MASK_PORTRAIT)
//            return false
//        }
        return true
    }

    private fun checkSize(nodeRect: Rect, windowRect: Rect, result: Result): Boolean {
        val nw = nodeRect.width().coerceAtLeast(nodeRect.height())
        val nh = nodeRect.width().coerceAtMost(nodeRect.height())
        val isPortrait = windowRect.width() < windowRect.height()
        result.portrait = isPortrait
        if (isPortrait) {
            if (nw == 0 || nw >= windowRect.width() / 3) {
                result.maskReason(Result.REASON_ILLEGAL_SIZE or Result.REASON_MASK_TRANSVERSE)
                return false
            }
            if (nh == 0 || nh >= windowRect.height() / 8) {
                result.maskReason(Result.REASON_ILLEGAL_SIZE or Result.REASON_MASK_PORTRAIT)
                return false
            }
        } else {
            if (nw == 0 || nw > windowRect.width() / 6) {
                result.maskReason(Result.REASON_ILLEGAL_SIZE or Result.REASON_MASK_TRANSVERSE)
                return false
            }
            if (nh == 0 || nh > windowRect.height() / 4) {
                result.maskReason(Result.REASON_ILLEGAL_SIZE or Result.REASON_MASK_PORTRAIT)
                return false
            }
        }
        return true
    }

    /**
     * Truncate the file's length to 0 and seek its r/w position to 0, namely, clear its content.
     */
    private fun truncate(pfd: ParcelFileDescriptor) {
        try {
            Os.ftruncate(pfd.fileDescriptor, 0)
            Os.lseek(pfd.fileDescriptor, 0, SEEK_SET)
        } catch (e: ErrnoException) {
            dumpError(e)
        }
    }

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            log("Service is dead at ${formatCurrentTime()}. Goodbye, world!")
            persistSkippingCount()
            persistRecords()
            persistLog()
        })
    }

    override fun persistLog() {
        if (recordQueue.size != 0) logFileDescriptor?.run {
            truncate(this)
            FileOutputStream(fileDescriptor).bufferedWriter().use { writer ->
                recordQueue.forEach {
                    writer.write(it)
                    writer.newLine()
                }
                writer.flush()
            }
        }
    }

    private fun persistSkippingCount() {
        if (skippingCount != -1) {
            countFileDescriptor?.run {
                truncate(this)
                ParcelFileDescriptor.AutoCloseOutputStream(this).bufferedWriter().use {
                    it.write(skippingCount.toString())
                    it.flush()
                }
            }
        }
    }

    private fun persistRecords() {
        recordFileDescriptor?.run {
            if (!records.isEmpty()) {
                truncate(this)
                ParcelFileDescriptor.AutoCloseOutputStream(this).bufferedWriter().use {
                    records.forEach { record ->
                        it.write(record.toString())
                        it.newLine()
                    }
                    it.flush()
                }
            }
        }
    }


    override fun destroy() {
        try {
            uiAutomationHidden.disconnect()
        } catch (t: Throwable) {
            dumpError(t)
        } finally {
            if (handlerThread.isAlive) {
                handlerThread.quitSafely()
            }
            exitProcess(0)
        }
    }
}