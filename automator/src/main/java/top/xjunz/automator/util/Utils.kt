package top.xjunz.automator.util

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author xjunz 2021/8/12
 */
const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
fun readStackTrace(t: Throwable): String {
    val out = ByteArrayOutputStream()
    t.printStackTrace(PrintStream(out))
    out.close()
    return out.toString()
}

private val dateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
}

fun formatTime(timestamp: Long): String = dateFormat.format(Date(timestamp))

fun formatCurrentTime(): String = dateFormat.format(Date(System.currentTimeMillis()))