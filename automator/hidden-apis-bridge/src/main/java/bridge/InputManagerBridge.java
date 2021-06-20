package bridge;

import android.hardware.input.InputManager;
import android.view.InputEvent;

/**
 * @author xjunz 2021/6/20 23:59
 */
public class InputManagerBridge {

    public static InputManager getInputManager(){
        return InputManager.getInstance();
    }
    /**
     * @see InputManager#injectInputEvent(InputEvent, int)
     */
    public static void injectInputEvent(InputEvent event) {
        InputManager.getInstance().injectInputEvent(event, InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }
}
