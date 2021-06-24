package top.xjunz.automator

import android.app.UiAutomation
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import rikka.shizuku.Shizuku
import top.xjunz.automator.databinding.ActivityMainBinding
import top.xjunz.library.automator.Automator
import top.xjunz.library.automator.AutomatorFactory
import top.xjunz.library.automator.IAutomatorConnection
import top.xjunz.library.automator.IOnAccessibilityEventListener
import top.xjunz.library.automator.impl.AutomatorConnection
import java.util.*

/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
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
        Shizuku.addBinderReceivedListenerSticky {
            //binding.tvOutput.text = "Binder received!"
        }
        Shizuku.addBinderDeadListener {
            //binding.tvOutput.text = "Binder dead!"
        }
        /*when (Shizuku.checkRemotePermission("android.permission.REAL_GET_TASKS")) {
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
                        }
                    }
                }.start()
            }
            PackageManager.PERMISSION_DENIED -> toast("Shizuku has no permission of INJECT_EVENTS.")*/
    }

    private var automatorService: IAutomatorConnection? = null
    private val userServiceStandaloneProcessArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, AutomatorConnection::class.java.name))
            .processNameSuffix("service").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)
    }
    private val userServiceConnection by lazy {
        //UiAutomation
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val res = StringBuilder()
                res.append("onServiceConnected: ")
                if (binder != null && binder.pingBinder()) {
                    automatorService = IAutomatorConnection.Stub.asInterface(binder)
                    try {
                        automatorService?.run {
                            res.append(sayHello())
                            if (!isConnnected) {
                                connect()
                            }
                            /* setOnAccessibilityEventListener(object : IOnAccessibilityEventListener.Stub() {
                                 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
                                     Log.i("XJUNZ", event!!.packageName.toString() + "/" + event.className)
                                     event.recycle()
                                 }
                             })*/
                        }
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                        res.append(Log.getStackTraceString(e))
                    }
                } else {
                    res.append("invalid binder for ").append(name).append(" received")
                }
               // binding.tvOutput.text = res.toString()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
               // binding.tvOutput.text = "${name?.flattenToString() ?: "Remote service"} died!"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
      //  automatorService?.run {
       //     disconnect()
      //      shutdown()
      //  }
        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, false)
    }

    fun bindAutomatorService(view: View) {
        val result = StringBuilder()
        try {
            if (Shizuku.getVersion() < 10) {
                result.append("requires Shizuku API 10")
            } else {
                Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            }
        } catch (tr: Throwable) {
            tr.printStackTrace()
            result.append(tr.toString())
        }
    }

    fun showTestPage(view: View) {
        supportFragmentManager.beginTransaction().add(R.id.root, TestFragment())
            .addToBackStack("test").commit()
    }

    fun shutdownService(view: View) {
        automatorService?.run {
            disconnect()
            shutdown()
        }
    }
}