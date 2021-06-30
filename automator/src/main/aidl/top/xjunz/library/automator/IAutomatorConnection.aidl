// IAutomatorConnection.aidl
package top.xjunz.library.automator;

import top.xjunz.library.automator.AutomatorConfig;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
interface IAutomatorConnection {

    void connect()=1;

    void disconnect()=2;

    String sayHello()=5;

    boolean isConnnected()=6;

    long getStartTimestamp()=8;

    void setConfig(in AutomatorConfig config)=9;

    void setShizukuBinder(in IBinder binder)=10;

    void destroy() = 16777114; // Destroy method defined by Shizuku server

}