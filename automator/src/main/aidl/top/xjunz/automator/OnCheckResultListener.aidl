// OnCheckResultListener.aidl
package top.xjunz.automator;

import top.xjunz.automator.model.Result;

interface OnCheckResultListener {
   void onCheckResult(in Result result);
}