package async.apf.model.events;

import async.apf.interfaces.IEvent;
import async.apf.model.enums.RobotEventType;

public class RobotEvent implements IEvent {
    private final RobotEventType eventType;
    private final int phase;
    private final int id;

    public RobotEvent(RobotEventType eventType, int phase, int id) {
        this.eventType = eventType;
        this.phase = phase;
        this.id = id;
    }

    public RobotEventType getEventType() {
        return this.eventType;
    }

    public int getPhase() {
        return phase;
    }
    
    public int getId() {
        return id;
    }

    @Override
    public String getEventName() {
        return eventType.name();
    }
}