package $android.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.IUiAutomationConnection;
import android.graphics.Bitmap;
import android.os.Looper;
import android.view.InputEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * @author xjunz 2021/6/23 0:48
 */
@SuppressWarnings("unused")
public class UiAutomation {
    public UiAutomation(Looper looper, IUiAutomationConnection connection) {
        throw new RuntimeException("Stub!");
    }

    public void connect() {
        throw new RuntimeException("Stub!");
    }

    public void disconnect() {
        throw new RuntimeException("Stub!");
    }

    public final AccessibilityServiceInfo getServiceInfo() {
        throw new RuntimeException("Stub!");
    }

    public final void setServiceInfo(AccessibilityServiceInfo info) {
        throw new RuntimeException("Stub!");
    }

    public void setOnAccessibilityEventListener(android.app.UiAutomation.OnAccessibilityEventListener listener) {
        throw new RuntimeException("Stub!");
    }

    public Bitmap takeScreenshot() {
        throw new RuntimeException("Stub!");
    }

    public AccessibilityNodeInfo getRootInActiveWindow() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Seems not working well on shell mode.
     * @see <a href="https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/app/UiAutomationConnection.java;drc=master;bpv=1;bpt=1;l=522?gsn=throwIfCalledByNotTrustedUidLocked&gs=kythe%3A%2F%2Fandroid.googlesource.com%2Fplatform%2Fsuperproject%3Flang%3Djava%3Fpath%3Dandroid.app.UiAutomationConnection%239c958fde171037a7b636674d1d8d5767332c52ccab8f1289f4ea5e46acd0d56e"/>
     */
    public boolean injectInputEvent(InputEvent event, boolean sync) {
        throw new RuntimeException("Stub!");
    }
}
