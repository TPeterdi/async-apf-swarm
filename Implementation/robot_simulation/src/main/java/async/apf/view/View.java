package async.apf.view;

import async.apf.model.Event;

public class View {

    // Method to handle an event and display appropriate message
    public void handleEvent(Event event) {
        if (event.isGlobalEvent()) {
            handleGlobalEvent(event);
        } else {
            handleRobotEvent(event);
        }
    }

    // Handle global events like SIMULATION_START and SIMULATION_END
    private void handleGlobalEvent(Event event) {
        switch (event.getEventType()) {
            case SIMULATION_START:
                System.out.println("View: Simulation has started!");
                break;
            case SIMULATION_END:
                System.out.println("View: Simulation has ended!");
                break;
            default:
                break;
        }
    }

    // Handle robot-specific events
    private void handleRobotEvent(Event event) {
        switch (event.getEventType()) {
            case LOOKING:
                System.out.println("View: Robot " + event.getRobotId() + " is looking around.");
                break;
            case COMPUTING:
                System.out.println("View: Robot " + event.getRobotId() + " is computing data.");
                break;
            case MOVING:
                System.out.println("View: Robot " + event.getRobotId() + " is moving from (" +
                        event.getFromX() + "," + event.getFromY() + ") to (" +
                        event.getToX() + "," + event.getToY() + ").");
                break;
            case IDLE:
                System.out.println("View: Robot " + event.getRobotId() + " is idle.");
                break;
            default:
                break;
        }
    }
}