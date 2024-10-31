package async.apf.view.events;


import java.util.List;

import async.apf.interfaces.IEvent;
import async.apf.model.Coordinate;
import async.apf.view.enums.ViewEventTypeR;

public class ViewCoordinatesEventR implements IEvent {
    private final ViewEventTypeR eventType;
    private final List<Coordinate> coordinates;

    public ViewCoordinatesEventR(ViewEventTypeR eventType, List<Coordinate> coordinates) {
        this.eventType = eventType;
        this.coordinates = coordinates;
    }

    public ViewEventTypeR getEventType() {
        return this.eventType;
    }
    public List<Coordinate> getCoordinates() {
        return this.coordinates;
    }
}
