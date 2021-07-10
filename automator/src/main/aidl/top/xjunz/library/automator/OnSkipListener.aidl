// OnSkipListener.aidl
package top.xjunz.library.automator;
import android.view.accessibility.AccessibilityNodeInfo;

interface OnSkipListener {
    void onSkip(int times,int type,in AccessibilityNodeInfo node);
}