package android.app;

import android.os.Looper;

import dev.rikka.tools.refine.RefineAs;

/**
 * @author xjunz 2021/6/23 0:48
 */
@SuppressWarnings("unused")
@RefineAs(UiAutomation.class)
public class UiAutomationHidden {
    /**
     * UiAutomation suppresses accessibility services by default. This flag specifies that
     * existing accessibility services should continue to run, and that new ones may start.
     * This flag is set when obtaining the UiAutomation from
     * {@link android.app.Instrumentation#getUiAutomation(int)}.
     */
    public static final int FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES = 0x00000001;

    public UiAutomationHidden(Looper looper, IUiAutomationConnection connection) {
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
}
