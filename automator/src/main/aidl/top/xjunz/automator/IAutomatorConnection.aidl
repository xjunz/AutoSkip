// IAutomatorConnection.aidl
package top.xjunz.automator;

import top.xjunz.automator.OnCheckResultListener;
import top.xjunz.automator.model.Result;
import top.xjunz.automator.model.Record;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import java.util.List;

interface IAutomatorConnection {

    String sayHello()=1;

    void startMonitoring()=2;

    boolean isMonitoring()=3;

    long getStartTimestamp()=8;

    int getPid()=10;

    int getSkippingCount()=11;

    void setFileDescriptors(in ParcelFileDescriptor[] pfds)=12;

    void setBasicEnvInfo(in String info)=13;

    void setSkippingCount(in int count)=14;

    void standaloneCheck(in OnCheckResultListener listener)=15;

    List<Record> getRecords()=16;

    void persistLog()=18;

    void destroy() = 16777114; // Destroy method defined by Shizuku server

}