package top.xjunz.automator.app

import android.app.Application
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import top.xjunz.automator.AutomatorConnection
import top.xjunz.automator.BuildConfig
import top.xjunz.automator.IAutomatorConnection
import top.xjunz.automator.OnCheckResultListener
import top.xjunz.automator.model.Record
import top.xjunz.automator.util.SHIZUKU_PACKAGE_NAME
import java.io.FileInputStream
import java.util.concurrent.TimeoutException


/**
 * A [ViewModel], which manipulating the automator service. This [ViewModel] is across the
 * [Application]'s lifecycle cuz it's stored in the [AutomatorApp.appViewModelStore]. We do so
 * because we want this [ViewModel] to be shared within activities.
 *
 * @author xjunz 2021/6/26
 */
class AutomatorViewModel constructor(val app: Application) : AndroidViewModel(app) {
    private val tag = "Automator"
    var initialized = false

    fun init() {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        readSkippingCount()
    }

    companion object {
        const val BINDING_SERVICE_TIMEOUT_MILLS = 5000L
        fun get(): AutomatorViewModel {
            return ViewModelProvider(AutomatorApp.me, ViewModelProvider.AndroidViewModelFactory(AutomatorApp.me))
                .get(AutomatorViewModel::class.java)
        }
    }

    /**
     * Whether we have got the Shizuku permission or not.
     */
    val isGranted = MutableLiveData<Boolean>()

    /**
     * Whether our service is running or not. When [isAvailable] becomes false, our service would be
     * killed expectedly. Meanwhile, [isGranted] will not affect this value once our service is started.
     */
    val isRunning = MutableLiveData<Boolean>()

    /**
     * Whether our service is under binding state.
     */
    val isBinding = MutableLiveData<Boolean>()

    /**
     * Whether the Shizuku service is started and we've obtained the binder or not.
     */
    val isAvailable = MutableLiveData<Boolean>()

    /**
     * Whether the shizuku manager is installed and enabled.
     */
    val isInstalled = MutableLiveData<Boolean>()

    /**
     * Whether the auto starter has tried to start the service on boot no matter whether it succeeded
     * or not.
     */
    val isAutoStarted = MutableLiveData<Boolean?>()

    val error = MutableLiveData<Throwable?>()

    val skippingTimes = MutableLiveData<Int?>()

    /**
     * The millisecond timestamp when our service is started.
     */
    var serviceStartTimestamp = -1L

    var pfds = arrayOfNulls<ParcelFileDescriptor>(3)

    /**
     * Check whether the Shizuku manager is installed on this device.
     */
    fun syncShizukuInstallationState() {
        isInstalled.value = runCatching {
            app.packageManager.getApplicationInfo(SHIZUKU_PACKAGE_NAME, PackageManager.GET_UNINSTALLED_PACKAGES)
        }.isSuccess
    }


    private val serviceNameSuffix = "service"
    private val serviceName = "${BuildConfig.APPLICATION_ID}:$serviceNameSuffix"

    /**
     * Bind a running service if any and kill all unbindable services created by us to avoid launching
     * multiple services.
     *
     * Shizuku has no build-in apis to judge whether a user service is still alive or not.
     * Then we have to fallback using the 'ps' cmd.
     *
     * V12: [Shizuku.peekUserService] is not safe in some cases.
     */
    @Suppress("Deprecation")
    fun bindRunningServiceOrKill() = viewModelScope.launch {
        isBinding.value = true
        var bound: Boolean? = null
        withContext(Dispatchers.IO) {
            Shizuku.newProcess(arrayOf("ps", "-A", "-o", "pid", "-o", "name"), null, "/")
                .apply {
                    inputStream.bufferedReader().useLines { sequence ->
                        sequence.forEach { line ->
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
        isRunning.value = bound == true
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

    private val deathRecipient: IBinder.DeathRecipient by lazy {
        IBinder.DeathRecipient {
            Log.i(tag, "The remote service is dead!")
            isRunning.postValue(false)
            Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, false)
        }
    }
    private var automatorService: IAutomatorConnection? = null
    private val userServiceConnection by lazy {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) = synchronized(lock) {
                if (binder != null && binder.pingBinder()) {
                    automatorService = IAutomatorConnection.Stub.asInterface(binder)
                    automatorService?.run {
                        try {
                            Log.i(tag, sayHello())
                            binder.linkToDeath(deathRecipient, 0)
                            if (!isMonitoring) {
                                setBasicEnvInfo(AutomatorApp.getBasicEnvInfo())
                                initFileDescriptors()
                                setFileDescriptors(pfds)
                                startMonitoring()
                                Log.i(tag, "Monitoring started successfully!")
                            }
                            skippingTimes.value = skippingCount
                            serviceStartTimestamp = startTimestamp
                            isRunning.value = true
                        } catch (t: Throwable) {
                            t.printStackTrace()
                            error.value = t
                        }
                    }
                }
                isBinding.value = false
                notified = true
                lock.notify()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isRunning.value = false
            }
        }
    }

    private val lock = Object()
    private var notified = false

    /**
     * Bind the remote service and wait for the binding result.
     */
    private fun bindServiceLocked(): Boolean = synchronized(lock) {
        try {
            Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
            lock.wait(BINDING_SERVICE_TIMEOUT_MILLS)
        } catch (t: Throwable) {
            t.printStackTrace()
            return false
        }
        return isRunning.value == true
    }

    /**
     * Bind the remote service and notify the [error] when something wrong happens.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun bindService() = viewModelScope.launch {
        try {
            isBinding.value = true
            withContext(Dispatchers.Default) {
                synchronized(lock) {
                    notified = false
                    Shizuku.bindUserService(userServiceStandaloneProcessArgs, userServiceConnection)
                    lock.wait(BINDING_SERVICE_TIMEOUT_MILLS)
                    if (!notified) throw TimeoutException("Timeout while connecting to the remote service")
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            error.value = t
            isBinding.value = false
        }
    }

    fun toggleService() {
        if (isRunning.value == true) {
            Shizuku.unbindUserService(userServiceStandaloneProcessArgs, userServiceConnection, true)
        } else {
            bindService()
        }
    }

    //https://youtrack.jetbrains.com/issue/KTIJ-838
    @Suppress("BlockingMethodInNonBlockingContext")
    fun readSkippingCount() = viewModelScope.launch {
        var times = 0
        withContext(Dispatchers.IO) {
            val file = app.getFileStreamPath(COUNT_FILE_NAME)
            if (file.exists()) {
                FileInputStream(file).bufferedReader().useLines {
                    times = it.firstOrNull()?.toIntOrNull() ?: 0
                }
            }
        }
        skippingTimes.value = times
    }

    private fun initFileDescriptors() {
        val mode = ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE
        pfds[0] = ParcelFileDescriptor.open(app.getFileStreamPath(COUNT_FILE_NAME), mode)
        pfds[1] = ParcelFileDescriptor.open(app.getFileStreamPath(LOG_FILE_NAME), mode)
        pfds[2] = ParcelFileDescriptor.open(app.getFileStreamPath(RECORD_FILE_NAME), mode)
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        isAvailable.value = true
        isGranted.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        if (isGranted.value == true) {
            bindRunningServiceOrKill()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        isAvailable.value = false
    }

    fun isServiceAlive(): Boolean = automatorService?.asBinder()?.pingBinder() ?: false

    private inline fun whenServiceIsAlive(block: IAutomatorConnection.() -> Unit) {
        if (isServiceAlive()) {
            try {
                block(automatorService!!)
            } catch (t: Throwable) {
                error.postValue(t)
            }
        }
    }

    fun dumpLog() = whenServiceIsAlive {
        persistLog()
    }

    fun launchStandaloneCheck(listener: OnCheckResultListener) = whenServiceIsAlive {
        standaloneCheck(listener)
    }

    fun updateSkippingCount() = whenServiceIsAlive {
        skippingTimes.value = skippingCount
    }


    fun updateGranted() {
        if (Shizuku.pingBinder()) {
            isGranted.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRecordListFromRemote(): MutableList<Record>? {
        whenServiceIsAlive {
            return records
        }
        return null
    }
}