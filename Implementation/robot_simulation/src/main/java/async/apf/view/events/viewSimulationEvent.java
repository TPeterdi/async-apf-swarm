package async.apf.view.events;

import async.apf.interfaces.IEvent;
import async.apf.view.enums.viewEventType;

public class viewSimulationEvent implements IEvent {

    private final viewEventType eventType;

    public viewSimulationEvent(viewEventType eventType) {this.eventType = eventType;}

    public viewEventType getEventType() {
        return eventType;
    }
}
