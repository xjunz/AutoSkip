// IAutomatorConnection.aidl
package top.xjunz.library.automator;

import top.xjunz.library.automator.AutomatorConfig;
import top.xjunz.library.automator.OnSkipListener;
import top.xjunz.library.automator.OnTestResultListener;
import top.xjunz.library.automator.Result;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

interface IAutomatorConnection {

    void connect()=1;

    void disconnect()=2;

    String sayHello()=5;

    boolean isConnected()=6;

    long getStartTimestamp()=8;

    void setConfig(in AutomatorConfig config)=9;

    int getPid()=10;

    int getSkippedTimes()=11;

    void setFileDescriptors(in ParcelFileDescriptor pfd)=12;

    void setOnSkipListener(in OnSkipListener listener)=13;

    void removeOnSkipListener()=14;

    Result testCheck()=15;

    void pause()=16;

    void resume()=17;

    void destroy() = 16777114; // Destroy method defined by Shizuku server

}