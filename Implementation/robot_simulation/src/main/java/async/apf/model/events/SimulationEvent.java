package async.apf.model.events;

import async.apf.interfaces.IEvent;
import async.apf.model.enums.SimulationEventType;

public class SimulationEvent implements IEvent {
    private final boolean isGlobal;  // Flag to identify if this is a global event

    private final int robotIndex;    // Robot identifier for robot-specific events
    private final SimulationEventType eventType;

    private final int phase;

    // For movement events
    private final int fromX;
    private final int fromY;
    private final int toX;
    private final int toY;

    // Constructor for robot-specific events
    public SimulationEvent(int robotIndex, SimulationEventType eventType, int phase, int fromX, int fromY, int toX, int toY) {
        this.isGlobal = false;

        this.robotIndex = robotIndex;
        this.eventType = eventType;
        this.phase = phase;

        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
    }

    // Constructor for global events
    public SimulationEvent(SimulationEventType eventType) {
        this.isGlobal = true;

        this.robotIndex = -1;
        this.eventType = eventType;
        this.phase = -1;

        this.fromX = 0;
        this.fromY = 0;
        this.toX = 0;
        this.toY = 0;
    }

    // Getter methods
    public int getRobotId() {
        return robotIndex;
    }

    public SimulationEventType getEventType() {
        return eventType;
    }

    public boolean isGlobalEvent() {
        return isGlobal;
    }

    public int getFromX() {
        return fromX;
    }

    public int getFromY() {
        return fromY;
    }

    public int getToX() {
        return toX;
    }

    public int getToY() {
        return toY;
    }

    public int getPhase() {
        return phase;
    }

    @Override
    public String toString() {
        if (isGlobal) {
            return "Global Event: " + eventType;
        } else if (eventType == SimulationEventType.ROBOT_MOVING) {
            return "Robot " + robotIndex + " is moving from (" + fromX + "," + fromY + ") to (" + toX + "," + toY + ")";
        } else {
            return "Robot " + robotIndex + ": " + eventType;
        }
    }

    @Override
    public String getEventName() {
        return eventType.name();
    }
}