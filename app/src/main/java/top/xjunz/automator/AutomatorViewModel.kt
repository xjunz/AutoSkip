package top.xjunz.automator

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.annotation.UiThread
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

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
}