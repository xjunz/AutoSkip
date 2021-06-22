package top.xjunz.library.automator.impl

import android.app.IActivityManager
import android.app.UiAutomation
import android.content.ComponentName
import android.content.Context
import android.hardware.input.IInputManager
import android.view.InputEvent
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import top.xjunz.library.automator.Automator

/**
 * @author xjunz 2021/6/22
 */
open class ShizukuAutomator : Automator() {
    object INSTANCE : ShizukuAutomator()

    private val inputManager by lazy {
        IInputManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.INPUT_SERVICE)))
    }
    private val activityManager by lazy {
        IActivityManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService(Context.ACTIVITY_SERVICE)))
    }

    override fun performClick(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun performActionUp(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun performActionDown(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    /**
     * Measured average time: 16ms
     * Device: Google Pixel 3
     */
    override fun injectInputEvent(event: InputEvent, mode: Int): Boolean {
        return inputManager.injectInputEvent(event, mode)
    }

    /**
     * Measured average time: 8ms
     * Device: Google Pixel 3
     */
    override fun getForegroundComponentName(): ComponentName? {
        return activityManager.getTasks(1)[0].topActivity
    }

    /*Or via transaction
    override fun injectInputEvent(event: InputEvent, mode: Int): Boolean {
        val data = SystemServiceHelper.obtainParcel(Context.INPUT_SERVICE, "android.hardware.input.IInputManager", "injectInputEvent")
        data.writeParcelable(event,Parcelable.PARCELABLE_WRITE_RETURN_VALUE )
        data.writeInt(mode)
        val reply = Parcel.obtain()
        var res = false
        try {
            Shizuku.transactRemote(data, reply, 0)
            reply.readException()
            res = reply.readBoolean()
        } catch (e: RemoteException) {
            Log.e("ShizukuAutomator", "InputManager#injectInputEvent", e)
        } finally {
            data.recycle()
            reply.recycle()
        }
        return res;
    }*/
}