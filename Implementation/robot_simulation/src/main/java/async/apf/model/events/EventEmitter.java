package async.apf.model.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import async.apf.interfaces.IEvent;
import async.apf.interfaces.IEventListener;

public class EventEmitter {
    private final List<IEventListener> listeners = new CopyOnWriteArrayList<>();
    private final HashMap<String, Runnable> onEventActions = new HashMap<>();

    // Method to add an event listener
    public void addEventListener(IEventListener listener) {
        listeners.add(listener);
    }

    // Method to remove an event listener
    public void removeEventListener(IEventListener listener) {
        listeners.remove(listener);
    }

    // Method to emit an event
    public synchronized void emitEvent(IEvent event) {
        // Notify all listeners about the event
        for (IEventListener listener : listeners) {
            listener.onEvent(event);
        }
        for (Map.Entry<String, Runnable> action : onEventActions.entrySet()) {
            if (action.getKey().equals(event.getEventName())) {
                action.getValue().run();
            }
        }
    }

    // Add a global on-event action
    public void onEvent(String eventName, Runnable action) {
        onEventActions.put(eventName, action);
    }
}