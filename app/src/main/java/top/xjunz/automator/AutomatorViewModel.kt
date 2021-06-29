package top.xjunz.automator

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import top.xjunz.library.automator.IAutomatorConnection
import top.xjunz.library.automator.impl.AutomatorConnection
import java.io.DataOutputStream


/**
 * @author xjunz 2021/6/26
 */
class AutomatorViewModel : ViewModel() {
    /**
     * Whether we have got the Shizuku permission or not. When [isAvailable] becomes false,
     * this value is also treated as false.
     */
    val isEnabled = MutableLiveData<Boolean>()

    /**
     * Whether our service is running or not. When [isAvailable] becomes false, our service will be
     * killed. Meanwhile, [isEnabled] will not affect this value once our service is started.
     */
    val isRunning = MutableLiveData<Boolean>()

    /**
     * Whether our service is under binding state.
     */
    val isBinding = MutableLiveData<Boolean>()

    /**
     * Whether the Shizuku service is started and we've obtained the binder or not. When this value
     * is false, it means either [isInstalled] is false or [isInstalled] is true while the shizuku
     * service is not started.
     */
    val isAvailable = MutableLiveData<Boolean>().apply {
        observeForever {
            if (it == false) {
                isEnabled.value = false
                isRunning.value = false
            }
        }
    }

    /**
     * Whether the Shizuku client is installed. When this is false, everything is false, of course.
     */
    val isInstalled = MutableLiveData<Boolean>()

    /**
     * The millisecond timestamp when our service is started.
     */
    var serviceStartTimestamp = -1L

    /**
     * Check whether the Shizuku client is installed on this device.
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun updateShizukuInstallationState() {
        viewModelScope.launch {
            isInstalled.value = Dispatchers.Default.invoke {
                AutomatorApp.appContext.packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES)
                    .find { it.packageName == AutomatorApp.SHIZUKU_PACKAGE_NAME } != null
            }
        }
    }

    private val serviceNameSuffix = "service"
    private val serviceName = "${BuildConfig.APPLICATION_ID}:$serviceNameSuffix"

    /**
     * "Destroy what you could not control."
     */
    @Suppress("DEPRECATION")
    fun bindRunningServiceOrKill() {
        viewModelScope.launch {
            var bound: Boolean? = null
            Dispatchers.IO.invoke {
                //Shizuku has no build-in apis to judge whether a user service
                //is still alive or not. Then we fallback using the 'ps' cmd.
                Shizuku.newProcess(arrayOf("ps", "-A", "-o", "pid", "-o", "name"), null, "/")
                    .apply {
                        val pid2kill = mutableListOf<String>()
                        inputStream.bufferedReader().useLines { sequence ->
                            sequence.forEach { line ->
                                //Try bound any service once, if one service has been bound, just kill other services
                                if (line.endsWith(serviceName) /*&& (bound == true || (bound == null &&
                                            bindServiceLocked().also { bound = it }.not()))*/) {
                                    pid2kill.add(line.trimIndent().split(" ")[0])
                                    //Shizuku.newProcess(arrayOf("kill", line.trimIndent().split(" ")[0]), null, "/")
                                }
                            }
                        }
                        pid2kill.forEach {
                            val out = DataOutputStream(outputStream)
                            out.writeBytes("kill $it\n")
                            out.flush()

                            out.writeBytes("exit\n")
                            out.flush()
                            waitFor()
                        }
                    }
            }
            isRunning.value = bound == true
        }
    }

    private val userServiceStandaloneProcessArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(BuildConfig.APPLICATION_ID, AutomatorConnection::class.java.name))
            .processNameSuffix(serviceNameSuffix).debuggable(BuildConfig.DEBUG).version(BuildConfig.VERSION_CODE)
    }

    private val userServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                synchronized(lock) {
                    if (binder != null && binder.pingBinder()) {
                        val automatorService = IAutomatorConnection.Stub.asInterface(binder)
                        try {
                            automatorService?.run {
                                Log.i("automator", sayHello())
                                if (!isConnnected) {
                                    connect()
                                }
                                serviceStartTimestamp = startTimestamp
                                isRunning.value = true
                                isEnabled.value = true
                            }
                        } catch (e: RemoteException) {
                            e.printStackTrace()
                        }
                    }
                    isBinding.value = false
                    lock.notifyAll()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isRunning.value = false
                updateShizukuInstallationState()
            }
        }
    }
    private val userServiceConnectionLocked by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                userServiceConnection.onServiceConnected(name, binder)
                synchronized(lock) {
                    lock.notify()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                userServiceConnection.onServiceDisconnected(name)
            }
        }
    }

    private val lock = Object()

    private fun bindServiceLocked(): Boolean = synchronized(lock) {
        try {
            isBinding.postValue(true)
            Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            lock.wait(6800)
        } catch (t: Throwable) {
            t.printStackTrace()
            return false
        }
        return isRunning.value == true
    }


    private fun bindService() = try {
        isBinding.value = true
        Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    private fun unbindService(): Boolean = try {
        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, true)
        isRunning.value = false
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    fun toggleService() {
        if (isRunning.value == true) {
            unbindService()
        } else {
            bindService()
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky {
            isAvailable.value = true
            isEnabled.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (isEnabled.value == true) {
                bindRunningServiceOrKill()
            }
        }
        Shizuku.addBinderDeadListener {
            isAvailable.value = false
            updateShizukuInstallationState()
        }
    }

}