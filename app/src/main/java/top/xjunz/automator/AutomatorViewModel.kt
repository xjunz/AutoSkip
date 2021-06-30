package top.xjunz.automator

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import rikka.shizuku.Shizuku
import top.xjunz.library.automator.IAutomatorConnection
import top.xjunz.library.automator.AutomatorConnection


/**
 * @author xjunz 2021/6/26
 */
class AutomatorViewModel : ViewModel() {
    private val tag = "Automator"

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
               //isRunning.value = false
                updateShizukuInstallationState()
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
    @Suppress("Deprecation")
    fun bindRunningServiceOrKill() = viewModelScope.launch {
        isBinding.value = true
        var bound: Boolean? = null
        withContext(Dispatchers.IO) {
            //Shizuku has no build-in apis to judge whether a user service
            //is still alive or not. Then we fallback using the 'ps' cmd.
            Shizuku.newProcess(arrayOf("ps", "-A", "-o", "pid", "-o", "name"), null, "/")
                .apply {
                    inputStream.bufferedReader().useLines { sequence ->
                        sequence.forEach { line ->
                            //Try bind any service once, if one service has been bound, just kill the other services
                            if (line.endsWith(serviceName)) {
                                val pid = line.trimIndent().split(" ")[0]
                                if (bound == null) {
                                    bound = bindServiceLocked()
                                    if (bound != true) {
                                        killProcess(pid)
                                    }
                                } else {
                                    killProcess(pid)
                                }
                            }
                        }
                    }
                }.destroy()
        }
        isBinding.value = false
    }

    @Suppress("Deprecation")
    private fun killProcess(pid: String) {
        //outputStream.write() seems not working, have to create a new process
        Shizuku.newProcess(arrayOf("kill", pid), null, "/").apply {
            waitFor()
            destroy()
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
                        automatorService?.run {
                            try {
                                Log.i(tag, sayHello())
                                setShizukuBinder(Shizuku.getBinder())
                                binder.linkToDeath(IBinder.DeathRecipient {
                                    //todo(当Shizuku服务停止时，此binder不一定死亡，重新打开Shizuku服务，
                                    // 会回调bindRunningServiceOrKill方法，最后在connect处抛出DeadObjectException，确认原因)
                                    Log.i(tag,binder.toString())
                                    isRunning.postValue(false)
                                    updateShizukuInstallationState()
                                }, 0)
                                if (!isConnnected) {
                                    connect()
                                }
                                Log.i(tag, "Automator connected successfully!")
                                serviceStartTimestamp = startTimestamp
                                isRunning.value = true
                            } catch (t: Throwable) {
                                t.printStackTrace()
                            }
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

    private val lock = Object()

    /**
     * Bind the service and wait the service to return the binding result.
     */
    private fun bindServiceLocked(): Boolean = synchronized(lock) {
        try {
            Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            lock.wait(6180)
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

    fun unbindService(kill: Boolean): Boolean = try {
        Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, kill)
        true
    } catch (t: Throwable) {
        t.printStackTrace()
        false
    }

    fun toggleService() {
        if (isRunning.value == true) {
            unbindService(true)
        } else {
            bindService()
        }
    }

    fun init() {
        Shizuku.addBinderReceivedListenerSticky {
            isAvailable.value = true
            isEnabled.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (isEnabled.value == true) {
                bindRunningServiceOrKill()
            }
        }
        Shizuku.addBinderDeadListener {
            isAvailable.value = false
        }
    }
}