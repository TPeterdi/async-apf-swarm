package async.apf.view.events;


import async.apf.interfaces.IEvent;
import async.apf.model.Coordinate;
import async.apf.view.enums.viewEventType;

import java.util.List;

public class viewCoordinatesEvent implements IEvent {
    private final viewEventType eventType;
    private final List<Coordinate> coordinates;

    public viewCoordinatesEvent(viewEventType eventType, List<Coordinate> coordinates) {
        this.eventType = eventType;
        this.coordinates = coordinates;
    }

    public viewEventType getEventType() {
        return this.eventType;
    }
    public List<Coordinate> getCoordinates() {
        return this.coordinates;
    }
}
