package android.view.accessibility;

import android.accessibilityservice.IAccessibilityServiceClient;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

/**
 * @author xjunz 2021/8/31
 */
public interface IAccessibilityManager extends IInterface {

    void unregisterUiTestAutomationService(IAccessibilityServiceClient client) throws RemoteException;

    abstract class Stub extends Binder implements IAccessibilityManager {

        public static IAccessibilityManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
