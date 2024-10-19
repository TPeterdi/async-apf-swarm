package async.apf.model.events;

import async.apf.interfaces.IEvent;
import async.apf.model.enums.RobotEventType;

public class RobotEvent implements IEvent {
    private final RobotEventType eventType;
    
    public RobotEvent(RobotEventType eventType) {
        this.eventType = eventType;
    }

    public RobotEventType getEventType() {
        return this.eventType;
    }
}