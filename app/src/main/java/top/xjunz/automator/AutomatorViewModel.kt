package top.xjunz.automator

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * @author xjunz 2021/6/26
 */
class AutomatorViewModel : ViewModel() {
    /**
     * Whether we have got the Shizuku permission or not
     */
    val isEnabled = MutableLiveData<Boolean>()

    /**
     * Whether our service is running or not
     */
    val isRunning = MutableLiveData<Boolean>()
}