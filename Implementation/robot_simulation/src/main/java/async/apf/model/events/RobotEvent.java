package async.apf.model.events;

import async.apf.interfaces.IEvent;
import async.apf.model.enums.RobotEventType;

public class RobotEvent implements IEvent {
    private final RobotEventType eventType;
    private final int id;

    public RobotEvent(RobotEventType eventType, int id) {
        this.eventType = eventType;
        this.id = id;
    }

    public RobotEventType getEventType() {
        return this.eventType;
    }
    
    public int getId() {
        return id;
    }
}