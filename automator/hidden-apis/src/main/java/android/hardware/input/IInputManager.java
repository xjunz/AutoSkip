package android.hardware.input;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;
import android.view.InputEvent;

/**
 * @author xjunz 2021/6/22
 */
public interface IInputManager extends IInterface {
    boolean injectInputEvent(InputEvent event, int mode) throws RemoteException;

    abstract class Stub extends Binder implements IInputManager {

        public static IInputManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
