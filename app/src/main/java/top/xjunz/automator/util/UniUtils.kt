package top.xjunz.automator.util

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import top.xjunz.automator.ALIPAY_DONATE_URL
import top.xjunz.automator.EMAIL_ADDRESS
import top.xjunz.automator.R
import top.xjunz.automator.app.AutomatorApp


/**
 * @author xjunz 2021/9/1
 */

fun viewUrl(context: Activity, url: String) {
    val intent = Intent(Intent.ACTION_VIEW).setData(Uri.parse(url))
    context.startActivity(Intent.createChooser(intent, null))
}

fun donate(context: Activity) {
    viewUrl(context, ALIPAY_DONATE_URL)
}

fun sendMailTo(context: Activity, log: Uri?) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_SUBJECT, AutomatorApp.me.getString(R.string.mail_subject, formatCurrentTime()))
        .putExtra(Intent.EXTRA_TEXT, AutomatorApp.me.getString(R.string.mail_body, AutomatorApp.getBasicEnvInfo()))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL_ADDRESS))
    if (log != null) {
        intent.putExtra(Intent.EXTRA_STREAM, log)
        val resInfoList: List<ResolveInfo> = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, log, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, null))
}