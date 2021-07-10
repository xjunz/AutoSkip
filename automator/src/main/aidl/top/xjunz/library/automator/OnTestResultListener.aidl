// OnTestResultListener.aidl
package top.xjunz.library.automator;

import top.xjunz.library.automator.Result;
import android.view.accessibility.AccessibilityNodeInfo;

interface OnTestResultListener {
   void onTestResult(in Result result);
}