// IRootAutomatorService.aidl
package top.xjunz.library.automator;
import android.view.InputEvent;

interface IRootAutomatorService {
    boolean injectInputEvent(out InputEvent event,int mode);
}