package async.apf.model.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import async.apf.interfaces.IEvent;
import async.apf.interfaces.IEventListener;

public class EventEmitter {
    private final List<IEventListener> listeners = new CopyOnWriteArrayList<>();

    // Method to add an event listener
    public void addEventListener(IEventListener listener) {
        listeners.add(listener);
    }

    // Method to remove an event listener
    public void removeEventListener(IEventListener listener) {
        listeners.remove(listener);
    }

    // Method to emit an event
    public void emitEvent(IEvent event) {
        // Notify all listeners about the event
        for (IEventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}