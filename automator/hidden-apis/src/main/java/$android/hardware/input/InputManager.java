package $android.hardware.input;

import android.view.InputEvent;

@SuppressWarnings("unused")
public final class InputManager {
    /**
     * Input Event Injection Synchronization Mode: None.
     * Never blocks.  Injection is asynchronous and is assumed always to be successful.
     */
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0; // see InputDispatcher.h

    /**
     * Input Event Injection Synchronization Mode: Wait for result.
     * Waits for previous events to be dispatched so that the input dispatcher can
     * determine whether input event injection will be permitted based on the current
     * input focus.  Does not wait for the input event to finish being handled
     * by the application.
     */
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT = 1;  // see InputDispatcher.h

    /**
     * Input Event Injection Synchronization Mode: Wait for finish.
     * Waits for the event to be delivered to the application and handled.
     */
    public static final int INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH = 2;  // see InputDispatcher.h

    public static InputManager getInstance() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Injects an input event into the event system on behalf of an application.
     * The synchronization mode determines whether the method blocks while waiting for
     * input injection to proceed.
     * <p>
     * Requires {@code android.Manifest.permission.INJECT_EVENTS} to inject into
     * windows that are owned by other applications.
     * </p><p>
     * Make sure you correctly set the event time and input source of the event
     * before calling this method.
     * </p>
     *
     * @param event The event to inject.
     * @param mode  The synchronization mode.  One of:
     *              {@link #INJECT_INPUT_EVENT_MODE_ASYNC},
     *              {@link #INJECT_INPUT_EVENT_MODE_WAIT_FOR_RESULT}, or
     *              {@link #INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH}.
     * @return True if input event injection succeeded.
     */
    public boolean injectInputEvent(InputEvent event, int mode) {
        throw new RuntimeException("Stub!");
    }


}
