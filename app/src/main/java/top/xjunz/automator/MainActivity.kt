package top.xjunz.automator

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants
import rikka.shizuku.ShizukuProvider
import rikka.shizuku.ShizukuRemoteProcess
import top.xjunz.automator.databinding.ActivityMainBinding
import top.xjunz.library.automator.IAutomatorConnection
import top.xjunz.library.automator.impl.AutomatorConnection
import top.xjunz.library.automator.impl.getRunningProcess
import java.util.*


/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by lazy {
        ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(AutomatorViewModel::class.java)
    }

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main).apply {
                lifecycleOwner = this@MainActivity
                vm = viewModel
            }
        initViews()
        initShizuku()
    }

    private val statusObserver by lazy {
        Observer<Boolean> {
            mainHandler.removeCallbacks(updateDurationTask)
            if (viewModel.isEnabled.value == true) {
                if (viewModel.isRunning.value == true) {
                    mainHandler.post(updateDurationTask)
                } else {
                    binding.tvCaptionStatus.setText(R.string.pls_start_service)
                }
            } else {
                binding.tvCaptionStatus.setText(R.string.pls_activate_service)
            }
        }
    }

    private fun initViews() {
        TopBarController(binding.topBar, binding.scrollView).init()
        viewModel.apply {
            isAvailable.value = false
            isEnabled.observe(this@MainActivity, statusObserver)
            isRunning.observe(this@MainActivity, statusObserver)
            updateShizukuInstallationState()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private val mainHandler by lazy {
        Handler(mainLooper)
    }

    private val updateDurationTask = object : Runnable {
        override fun run() {
            if (viewModel.serviceStartTimestamp > 0) {
                (System.currentTimeMillis() - viewModel.serviceStartTimestamp).let {
                    binding.tvCaptionStatus.text =
                        String.format(getString(R.string.format_running_duration), it / 3_600_000, it / 60_000 % 60, it / 1000 % 60)
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    private fun initShizuku() {
        Shizuku.addBinderReceivedListenerSticky {
            viewModel.apply {
                isAvailable.value = true
                isEnabled.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                try {
                    if (config.isLastRunning()) {
                        Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
                    } else {
                        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, true)
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
        Shizuku.addBinderDeadListener {
            viewModel.apply {
                isAvailable.value = false
                updateShizukuInstallationState()
            }
        }
    }

    private var automatorService: IAutomatorConnection? = null
    private val userServiceStandaloneProcessArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, AutomatorConnection::class.java.name)).processNameSuffix("service").debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)
    }
    private val userServiceConnection by lazy { //UiAutomation
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder != null && binder.pingBinder()) {
                    automatorService = IAutomatorConnection.Stub.asInterface(binder)
                    try {
                        automatorService?.run {
                            Log.i("automator", sayHello())
                            if (!isConnnected) {
                                connect()
                            }
                            viewModel.serviceStartTimestamp = startTimestamp
                            mainHandler.post(updateDurationTask)
                            viewModel.isRunning.value = true
                            viewModel.isEnabled.value = true
                        }
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
                viewModel.isBinding.value = false
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                viewModel.isRunning.value = false
                viewModel.updateShizukuInstallationState()
            }
        }
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

    fun showMenu(view: View) {}

    fun toggleService(view: View) {
        if (viewModel.isRunning.value == true) {
            Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, true)
            viewModel.isRunning.value = false
        } else {
            Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            viewModel.isBinding.value = true
        }
    }

    private val downloadUrl by lazy {
        when (resources.configuration.locale.script) {
            "Hans" -> "https://shizuku.rikka.app/zh-hans/download/"
            "Hant" -> "https://shizuku.rikka.app/zh-hant/download/"
            else -> "https://shizuku.rikka.app/download/"
        }
    }

    fun performShizukuAction(view: View) {
        if (viewModel.isInstalled.value == true) {
            packageManager.getLaunchIntentForPackage(AutomatorApp.SHIZUKU_PACKAGE_NAME)?.let {
                startActivity(it)
            }
        } else {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)), null))
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isAvailable.value == true) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                viewModel.isEnabled.value = true
            }
        }
    }

    fun requestPermission(view: View) {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            packageManager.getLaunchIntentForPackage(AutomatorApp.SHIZUKU_PACKAGE_NAME)?.let {
                startActivity(it)
            }
            toast(getString(R.string.pls_grant_manually))
        } else {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
                Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                    viewModel.isEnabled.value =
                        requestCode == SHIZUKU_PERMISSION_REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED
                }
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            } else {
                viewModel.isEnabled.value = true
            }
        }
    }
}