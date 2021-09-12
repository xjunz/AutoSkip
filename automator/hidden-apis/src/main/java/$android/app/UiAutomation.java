package $android.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.IUiAutomationConnection;
import android.graphics.Bitmap;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * @author xjunz 2021/6/23 0:48
 */
@SuppressWarnings("unused")
public class UiAutomation {
    /**
     * UiAutomation suppresses accessibility services by default. This flag specifies that
     * existing accessibility services should continue to run, and that new ones may start.
     * This flag is set when obtaining the UiAutomation from
     * {@link android.app.Instrumentation#getUiAutomation(int)}.
     */
    public static final int FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES = 0x00000001;

    public UiAutomation(Looper looper, IUiAutomationConnection connection) {
        throw new RuntimeException("Stub!");
    }

    public void connect() {
        throw new RuntimeException("Stub!");
    }

    public void connect(int flag) {
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
}
