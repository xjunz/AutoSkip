// IAutomatorConnection.aidl
package top.xjunz.library.automator;

import top.xjunz.library.automator.IOnAccessibilityEventListener;
import android.view.accessibility.AccessibilityNodeInfo;
interface IAutomatorConnection {

    void connect();

    void disconnect();

    Bitmap takeScreenshot(in Rect crop, int rotation);

    // Called from the system process.
    oneway void shutdown();

    void setOnAccessibilityEventListener(in IOnAccessibilityEventListener client);

    String sayHello();

    boolean isConnnected();

    AccessibilityNodeInfo getRootInActiveWindow();
}