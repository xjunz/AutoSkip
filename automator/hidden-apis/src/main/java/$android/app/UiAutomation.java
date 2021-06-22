package $android.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.IUiAutomationConnection;
import android.os.Looper;

/**
 * @author xjunz 2021/6/23 0:48
 */
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
}
