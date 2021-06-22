package top.xjunz.automator

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import rikka.shizuku.Shizuku
import top.xjunz.library.automator.Automator
import top.xjunz.library.automator.AutomatorFactory
import java.util.*
import kotlin.system.measureTimeMillis

/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity : AppCompatActivity() {
    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                    toast("Shizuku permission granted!")
                    init()
                } else {
                    toast("Please grant the shizuku permission!")
                    finish()
                }
            }
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        } else {
            init()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun init() {
        when (Shizuku.checkRemotePermission("android.permission.REAL_GET_TASKS")) {
            PackageManager.PERMISSION_GRANTED -> {
                val automator: Automator = AutomatorFactory.getAutomator(AutomatorFactory.Mode.SHIZUKU)
                object : Thread() {
                    var lastComponentName: ComponentName? = null
                    override fun run() {
                        super.run()
                        while (true) {
                            automator.getForegroundComponentName()?.run {
                                if (!Objects.equals(this, lastComponentName)) {
                                    Log.i("XJUNZ", flattenToString())
                                    lastComponentName = this
                                }
                                //AccessibilityNodeInfo().findAccessibilityNodeInfosByText()
                                if (className.contains("Splash")) {
                                    Log.i("XJUNZ", flattenToString())
                                    automator.run {
                                        val downTime = SystemClock.uptimeMillis()
                                        injectInputEvent(MotionEvent.obtain(downTime, downTime,
                                            MotionEvent.ACTION_DOWN, 930f, 1900f, 0).apply {
                                            source = InputDevice.SOURCE_TOUCHSCREEN
                                        }, Automator.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
                                        injectInputEvent(MotionEvent.obtain(downTime, downTime + 50,
                                            MotionEvent.ACTION_UP, 930f, 1900f, 0).apply {
                                            source = InputDevice.SOURCE_TOUCHSCREEN
                                        }, Automator.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
                                        Log.i("XJUNZ", "Click event injected!")
                                    }
                                }
                            }
                            /*val downTime = SystemClock.uptimeMillis()
                            automator.run {
                                injectInputEvent(MotionEvent.obtain(downTime, downTime,
                                    MotionEvent.ACTION_DOWN, 500f, 500f, 0).apply {
                                    source = InputDevice.SOURCE_TOUCHSCREEN
                                }, Automator.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
                                injectInputEvent(MotionEvent.obtain(downTime, downTime + 100,
                                    MotionEvent.ACTION_UP, 500f, 500f, 0).apply {
                                    source = InputDevice.SOURCE_TOUCHSCREEN
                                }, Automator.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH)
                            }*/
                        }
                    }
                }.start()
            }
            PackageManager.PERMISSION_DENIED -> toast("Shizuku has no permission of INJECT_EVENTS.")
        }
    }
}