package async.apf.view.events;

import async.apf.interfaces.IEvent;
import async.apf.view.enums.ViewEventType;

public class ViewSimulationEvent implements IEvent {
    private final ViewEventType eventType;
    private int delay;

    public ViewSimulationEvent(ViewEventType eventType) {this.eventType = eventType;}
    public ViewSimulationEvent(ViewEventType eventType, int delay) {
        this.eventType = eventType;
        this.delay = delay;
    }

    public ViewEventType getEventType() {
        return eventType;
    }

    public int getDelay() {
        return delay;
    }

    @Override
    public String getEventName() {
        return eventType.name();
    }
}
