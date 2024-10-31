package async.apf.view.events;

import async.apf.interfaces.IEvent;
import async.apf.view.enums.ViewEventTypeR;

public class ViewSimulationEventR implements IEvent {

    private final ViewEventTypeR eventType;
    private int delay;

    public ViewSimulationEventR(ViewEventTypeR eventType) {this.eventType = eventType;}
    public ViewSimulationEventR(ViewEventTypeR eventType, int delay) {
        this.eventType = eventType;
        this.delay = delay;
    }

    public ViewEventTypeR getEventType() {
        return eventType;
    }

    public int getDelay() {
        return delay;
    }
}
