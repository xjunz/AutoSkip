package top.xjunz.automator.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.shizuku.Shizuku
import rikka.sui.Sui
import top.xjunz.automator.*
import top.xjunz.automator.app.*
import top.xjunz.automator.autostart.enableShizukuAutoStart
import top.xjunz.automator.autostart.isAutoStartEnabled
import top.xjunz.automator.autostart.isShizukuAutoStartEnabled
import top.xjunz.automator.autostart.setAutoStartComponentEnable
import top.xjunz.automator.databinding.ActivityMainBinding
import top.xjunz.automator.stats.StatsActivity
import top.xjunz.automator.test.TestActivity
import top.xjunz.automator.util.*
import java.io.*
import java.util.*


/**
 * @author xjunz 2021/6/20 21:05
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel by lazy {
        AutomatorViewModel.get()
    }

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .apply {
                lifecycleOwner = this@MainActivity
                vm = viewModel
            }
        initViews()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.data?.host == "about") {
            AboutFragment().show(supportFragmentManager, "about")
        }
    }

    private val statusObserver by lazy {
        Observer<Boolean> {
            viewModel.run {
                val enabled = isAvailable.value == true && isGranted.value == true
                binding.btnRun.isEnabled = isRunning.value == true || (isBinding.value != true && enabled)
                mainHandler.removeCallbacks(updateDurationTask)
                if (isRunning.value == true) {
                    mainHandler.post(updateDurationTask)
                } else {
                    if (enabled) {
                        binding.tvCaptionStatus.setText(R.string.hint_start_service)
                    } else {
                        binding.tvCaptionStatus.setText(R.string.pls_activate_service)
                    }
                }
            }
        }
    }

    private val popupMenu by lazy {
        PopupMenu(this, binding.ibMenu, Gravity.NO_GRAVITY).apply {
            menuInflater.inflate(R.menu.main, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.item_dump_log -> {
                        viewModel.dumpLog()
                        if (!getFileStreamPath(LOG_FILE_NAME).exists()) {
                            toast(getString(R.string.no_log))
                            return@setOnMenuItemClickListener true
                        }
                        val uri = FileProvider.getUriForFile(
                            this@MainActivity,
                            "top.xjunz.automator.provider.file", logFile
                        )
                        if (BuildConfig.DEBUG) {
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                .addCategory(Intent.CATEGORY_DEFAULT)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(Intent.createChooser(intent, null))
                        } else {
                            val intent = Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uri)
                                .setType("text/plain")
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(Intent.createChooser(intent, null))
                        }
                    }
                    R.id.item_auto_start -> if (it.isChecked) {
                        setAutoStartComponentEnable(false)
                    } else {
                        if (Sui.isSui()) {
                            setAutoStartComponentEnable(true)
                        } else {
                            if (isShizukuAutoStartEnabled() || enableShizukuAutoStart()) {
                                setAutoStartComponentEnable(true)
                            } else {
                                toast(getString(R.string.pls_turn_on_shizuku_auto_start))
                                launchShizukuManager()
                            }
                        }
                        it.isChecked = isAutoStartEnabled()
                    }
                    R.id.item_feedback_email -> {
                        viewModel.dumpLog()
                        if (getFileStreamPath(LOG_FILE_NAME).exists()) {
                            val uri = FileProvider.getUriForFile(this@MainActivity, "top.xjunz.automator.provider.file", logFile)
                            sendMailTo(this@MainActivity, uri)
                        } else {
                            sendMailTo(this@MainActivity, null)
                        }
                    }
                    R.id.item_feedback_group -> viewUrl(this@MainActivity, FEEDBACK_GROUP_URL)
                    R.id.item_about -> AboutFragment().show(supportFragmentManager, "about")
                }
                return@setOnMenuItemClickListener true
            }
        }
    }
    private val logFile by lazy {
        getFileStreamPath(LOG_FILE_NAME)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {
        TopBarController(binding.topBar, binding.scrollView).init()
        binding.root.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        binding.ibMenu.setOnTouchListener(popupMenu.dragToOpenListener)
        viewModel.apply {
            isGranted.observe(this@MainActivity, statusObserver)
            isRunning.observe(this@MainActivity, statusObserver)
            isAvailable.observe(this@MainActivity, statusObserver)
            isBinding.observe(this@MainActivity, statusObserver)
            error.observe(this@MainActivity) {
                if (it != null) {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(R.string.error_occurred)
                        .setPositiveButton(android.R.string.ok, null)
                        .setMessage(it.stackTraceToString()).show()
                    error.value = null
                }
            }
            isAutoStarted.observe(this@MainActivity) {
                if (it != null && it) {
                    if (isRunning.value == true) {
                        toast(getString(R.string.auto_started_for_u))
                    } else {
                        toast(getString(R.string.auto_starting_for_u))
                    }
                    isAutoStarted.value = null
                }
            }
            if (!initialized) init()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncShizukuInstallationState()
        viewModel.updateGranted()
        viewModel.updateSkippingCount()
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
                    binding.tvCaptionStatus.text = String.format(
                        getString(R.string.format_running_duration),
                        it / 3_600_000, it / 60_000 % 60, it / 1000 % 60
                    )
                }
            }
            mainHandler.postDelayed(this, 1000)
        }
    }

    fun showMenu(view: View) {
        popupMenu.menu.findItem(R.id.item_auto_start).isChecked = isAutoStartEnabled()
        popupMenu.show()
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
            launchShizukuManager()
        } else {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)), null))
        }
    }

    private fun launchShizukuManager() {
        packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)?.let {
            startActivity(it)
        }
    }

    fun requestPermission(view: View) {
        if (Shizuku.shouldShowRequestPermissionRationale()) {
            toast(getString(R.string.pls_grant_manually))
        } else {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_DENIED) {
                Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                    viewModel.isGranted.value =
                        requestCode == SHIZUKU_PERMISSION_REQUEST_CODE && grantResult == PackageManager.PERMISSION_GRANTED
                }
                Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
            } else {
                viewModel.isGranted.value = true
            }
        }
    }

    fun testAvailability(view: View) {
        if (viewModel.isServiceAlive()) {
            startActivity(Intent(this, TestActivity::class.java))
        } else {
            toast(getString(R.string.pls_start_service))
        }
    }

    fun showRecords(view: View) {
        if (viewModel.skippingTimes.value != 0) {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }
}