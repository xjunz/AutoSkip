// IAutomatorConnection.aidl
package top.xjunz.library.automator;

import top.xjunz.library.automator.IOnAccessibilityEventListener;
import android.view.accessibility.AccessibilityNodeInfo;
interface IAutomatorConnection {

    void connect()=1;

    void disconnect()=2;

    Bitmap takeScreenshot(in Rect crop, int rotation)=3;

    void setOnAccessibilityEventListener(in IOnAccessibilityEventListener client)=4;

    String sayHello()=5;

    boolean isConnnected()=6;

    AccessibilityNodeInfo getRootInActiveWindow()=7;

    long getStartTimestamp()=8;

    void destroy() = 16777114; // Destroy method defined by Shizuku server

}