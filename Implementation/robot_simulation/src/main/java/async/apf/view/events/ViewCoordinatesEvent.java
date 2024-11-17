package async.apf.view.events;

import java.util.ArrayList;
import java.util.List;

import async.apf.interfaces.IEvent;
import async.apf.model.Coordinate;
import async.apf.view.enums.ViewEventType;

public class ViewCoordinatesEvent implements IEvent {
    private final ViewEventType eventType;
    private final List<Coordinate> coordinates;

    public ViewCoordinatesEvent(ViewEventType eventType, List<Coordinate> coordinates) {
        this.eventType = eventType;
        this.coordinates = new ArrayList<>();
        for (Coordinate coordinate : coordinates) {
            this.coordinates.add(coordinate);
        }
    }

    public ViewEventType getEventType() {
        return this.eventType;
    }
    
    public List<Coordinate> getCoordinates() {
        return this.coordinates;
    }

    @Override
    public String getEventName() {
        return eventType.name();
    }
}
