package top.xjunz.automator.autostart

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import top.xjunz.automator.app.AutomatorViewModel

/**
 * @author xjunz 2021/8/16
 */
class AutoStarter : BroadcastReceiver() {

    private val oneShotBinderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            AutomatorViewModel.get().run {
                app.openFileOutput("on_boot", Context.MODE_PRIVATE).bufferedWriter().apply {
                    write("binder received")
                    newLine()
                    write("permission granted: ${Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED}")
                    newLine()
                    write("is_binding: ${isBinding.value}")
                    newLine()
                    write("is_running: ${isRunning.value}")
                    newLine()
                    if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED &&
                        isBinding.value != true && isRunning.value != true
                    ) {
                        write("starting service...")
                        toggleService()
                        isAutoStarted.value = true
                    }
                }
            }
            Shizuku.removeBinderReceivedListener(this)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Shizuku.addBinderReceivedListenerSticky(oneShotBinderReceivedListener)
        }
    }
}