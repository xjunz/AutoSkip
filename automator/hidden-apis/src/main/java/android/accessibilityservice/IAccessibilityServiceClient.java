package android.accessibilityservice;

/**
 * @author xjunz 2021/6/22
 */
public interface IAccessibilityServiceClient {
    void init(in IAccessibilityServiceConnection connection, int connectionId, IBinder windowToken);

    void onAccessibilityEvent(in AccessibilityEvent event, in boolean serviceWantsEvent);

    void onInterrupt();

    void onGesture(int gesture);

    void clearAccessibilityCache();

    void onKeyEvent(in KeyEvent event, int sequence);

    void onMagnificationChanged(in Region region, float scale, float centerX, float centerY);

    void onSoftKeyboardShowModeChanged(int showMode);

    void onPerformGestureResult(int sequence, boolean completedSuccessfully);

    void onFingerprintCapturingGesturesChanged(boolean capturing);

    void onFingerprintGesture(int gesture);

    void onAccessibilityButtonClicked();

    void onAccessibilityButtonAvailabilityChanged(boolean available);
}
