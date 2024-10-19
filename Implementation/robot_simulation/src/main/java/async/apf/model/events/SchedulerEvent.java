package async.apf.model.events;

import java.util.UUID;

import async.apf.interfaces.IEvent;

public class SchedulerEvent implements IEvent {
    private final UUID uuid;
    
    public SchedulerEvent(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }
}