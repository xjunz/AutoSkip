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

    public boolean injectInputEvent(InputEvent event, boolean sync) {
        throw new RuntimeException("Stub!");
    }
}
