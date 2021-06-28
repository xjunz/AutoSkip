package top.xjunz.library.automator.impl

import android.app.IActivityManager
import android.content.Context
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * @author xjunz 2021/6/28 23:08
 */
private val activityManager by lazy {
    IActivityManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))
}

fun getRunningProcess() = activityManager.runningAppProcesses