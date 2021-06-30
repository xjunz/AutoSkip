package top.xjunz.library.automator

import `$android`.hardware.input.InputManager
import android.os.Build
import android.os.Looper
import android.view.InputEvent

/**
 * @author xjunz 2021/6/21
 */
class RootAutomatorService  {
    companion object {
        val DEBUG_ARGS by lazy {
            val sdk = Build.VERSION.SDK_INT
            when {
                sdk >= 30 -> "-Xcompiler-option --debuggable -XjdwpProvider:adbconnection " +
                        "-XjdwpOptions:suspend=n,server=y"
                sdk >= 28 -> "-Xcompiler-option --debuggable -XjdwpProvider:internal " +
                        "-XjdwpOptions:transport=dt_android_adb,suspend=n,server=y"
                else -> "-Xcompiler-option --debuggable -agentlib:jdwp=transport=dt_android_adb," +
                        "suspend=n,server=y"
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            Looper.prepare()
            RootAutomatorService()
            Looper.loop()
        }
    }

    fun getServiceLauncherCommand(classPath: String, debuggable: Boolean): String {
        val debugArgs = if (debuggable) {
            " $DEBUG_ARGS"
        } else {
            ""
        }

        return "CLASSPATH=${classPath} /system/bin/app_process$debugArgs" +
                " -Djava.library.path=${classPath} /system/bin" +
                " --nice-name=root_automator top.xjunz.automator.ServiceLauncher"
    }

     fun injectInputEvent(event: InputEvent?, mode: Int) = InputManager.getInstance().injectInputEvent(event, mode)
}