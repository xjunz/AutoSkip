// IOnAccessibilityEventListener.aidl
package top.xjunz.library.automator;

// Declare any non-default types here with import statements
import android.view.accessibility.AccessibilityEvent;
interface IOnAccessibilityEventListener {
    /**
         * Callback for receiving an {@link AccessibilityEvent}.
          * <p>
          * <strong>Note:</strong> This method is <strong>NOT</strong> executed
          * on the main test thread. The client is responsible for proper
          * synchronization.
          * </p>
          * <p>
          * <strong>Note:</strong> It is responsibility of the client
          * to recycle the received events to minimize object creation.
          * </p>
          *
          * @param event The received event.
          */
          void onAccessibilityEvent(in AccessibilityEvent event);
}