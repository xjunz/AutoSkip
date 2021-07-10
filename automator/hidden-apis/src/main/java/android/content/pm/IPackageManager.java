package android.content.pm;

import android.app.IActivityManager;
import android.content.ComponentName;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

import java.util.List;

/**
 * @author xjunz 2021/7/8
 */
public interface IPackageManager extends IInterface {
    ComponentName getHomeActivities(List<ResolveInfo> outHomeCandidates);

    abstract class Stub extends Binder implements IPackageManager {

        public static IPackageManager asInterface(IBinder obj) {
            throw new RuntimeException("Stub!");
        }
    }
}
