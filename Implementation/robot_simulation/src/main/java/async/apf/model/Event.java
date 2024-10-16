package async.apf.model;

public class Event {
    private String robotId;    // Robot identifier for robot-specific events
    private RobotEventType eventType;
    private int fromX, fromY, toX, toY; // For movement events
    private boolean isGlobal;  // Flag to identify if this is a global event

    // Constructor for robot-specific events (with movement data)
    public Event(String robotId, RobotEventType eventType, int fromX, int fromY, int toX, int toY) {
        this.robotId = robotId;
        this.eventType = eventType;
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.isGlobal = false;
    }

    // Constructor for global events
    public Event(RobotEventType eventType) {
        this.eventType = eventType;
        this.isGlobal = true;
    }

    // Getter methods
    public String getRobotId() {
        return robotId;
    }

    public RobotEventType getEventType() {
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

    @Override
    public String toString() {
        if (isGlobal) {
            return "Global Event: " + eventType;
        } else if (eventType == RobotEventType.MOVING) {
            return "Robot " + robotId + " is moving from (" + fromX + "," + fromY + ") to (" + toX + "," + toY + ")";
        } else {
            return "Robot " + robotId + ": " + eventType;
        }
    }
}