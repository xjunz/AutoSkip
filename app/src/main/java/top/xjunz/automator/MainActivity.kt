package top.xjunz.automator

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.OneShotPreDrawListener
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import com.google.android.material.shape.MaterialShapeDrawable
import rikka.shizuku.Shizuku
import top.xjunz.automator.databinding.ActivityMainBinding
import top.xjunz.library.automator.IAutomatorConnection
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

        initViews()
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

    private fun initViews() {
        val cornerSize = resources.getDimension(R.dimen.corner_item)
        val margin = Utils.dp2px(8)
        val z = Utils.dp2px(3)
        val mtrl = MaterialShapeDrawable().apply {
            setCornerSize(cornerSize)
            strokeColor = getColorStateList(R.color.material_on_surface_stroke)
            strokeWidth = Utils.dp2px(1)
            fillColor = Utils.getAttributeColorStateList(this@MainActivity, android.R.attr.colorBackground)
            requiresCompatShadow()
            setShadowColor(0xA0A0A0)
            shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS
        }
        val back = InsetDrawable(mtrl, 0, 0, 0, z.toInt())
        binding.topBar.apply {
            background = back
            bringToFront()
            setOnApplyWindowInsetsListener { _, insets ->
                OneShotPreDrawListener.add(this) {
                    binding.scrollView.setPadding(0, height, 0, insets.systemWindowInsetBottom)
                }
                setPadding(0, insets.systemWindowInsetTop, 0, z.toInt())
                return@setOnApplyWindowInsetsListener insets
            }
        }
        var lastScrollY = 0
        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            var range: IntRange? = null
            if (lastScrollY < margin && scrollY >= margin) {
                range = 0..1
            } else if (lastScrollY > margin && scrollX <= margin) {
                range = IntRange(1, 0)
            }
            if (range == null) {
                return@OnScrollChangeListener
            }
            ValueAnimator.ofFloat(range.first.toFloat(), range.last.toFloat()).apply {
                addUpdateListener {
                    val f = it.animatedFraction
                    mtrl.apply {
                        setCornerSize(cornerSize * (1 - f))
                        strokeWidth = 3f * (1 - f)
                        elevation = 9 * f
                    }
                    binding.topBar.apply {
                        layoutParams = (layoutParams as ViewGroup.MarginLayoutParams).apply {
                            marginStart = ((1 - f) * margin).toInt()
                            marginEnd = ((1 - f) * margin).toInt()
                        }
                    }
                }
            }.start()

            lastScrollY = scrollY
        })
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun init() {
        Shizuku.addBinderReceivedListenerSticky { //binding.tvOutput.text = "Binder received!"
        }
        Shizuku.addBinderDeadListener { //binding.tvOutput.text = "Binder dead!"
        }/*when (Shizuku.checkRemotePermission("android.permission.REAL_GET_TASKS")) {
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
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, AutomatorConnection::class.java.name)).processNameSuffix("service").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)
    }
    private val userServiceConnection by lazy { //UiAutomation
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
                            }/* setOnAccessibilityEventListener(object : IOnAccessibilityEventListener.Stub() {
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
                } // binding.tvOutput.text = res.toString()
            }

            override fun onServiceDisconnected(name: ComponentName?) { // binding.tvOutput.text = "${name?.flattenToString() ?: "Remote service"} died!"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy() //  automatorService?.run {
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
        supportFragmentManager.beginTransaction().add(R.id.scroll_view, TestFragment()).addToBackStack("test").commit()
    }

    fun shutdownService(view: View) {
        automatorService?.run {
            disconnect()
            shutdown()
        }
    }

    fun showMenu(view: View) {}
}