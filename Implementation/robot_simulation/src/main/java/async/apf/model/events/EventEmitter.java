package async.apf.model.events;

import java.util.ArrayList;
import java.util.List;

import async.apf.interfaces.IEvent;
import async.apf.interfaces.IEventListener;

public class EventEmitter {
    private final List<IEventListener> listeners = new ArrayList<>();

    // Method to add an event listener
    public void addEventListener(IEventListener listener) {
        listeners.add(listener);
    }

    // Method to emit an event
    public void emitEvent(IEvent event) {
        // Notify all listeners about the event
        for (IEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}