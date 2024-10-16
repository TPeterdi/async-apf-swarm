package async.apf.model;

import java.util.concurrent.CompletableFuture;

public class Robot {
    private String robotId;
    private int currentX, currentY;

    public Robot(String robotId, int startX, int startY) {
        this.robotId = robotId;
        this.currentX = startX;
        this.currentY = startY;
    }

    // Simulate a robot performing actions asynchronously
    public CompletableFuture<Event> performActions() {
        
        // Robot starts with LOOKING event
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep((long) (Math.random() * 1000));
                return new Event(robotId, RobotEventType.LOOKING, 0, 0, 0, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        })
        // COMPUTE
        .thenCompose(event -> {
            display(event);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                    return new Event(robotId, RobotEventType.COMPUTING, 0, 0, 0, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            });
        })
        // MOVE
        .thenCompose(event -> {
            display(event);
            return CompletableFuture.supplyAsync(() -> {
                // Robot emits MOVING event with coordinates
                int newX = currentX + (int) (Math.random() * 10); // Random new position
                int newY = currentY + (int) (Math.random() * 10);
                Event moveEvent = new Event(robotId, RobotEventType.MOVING, currentX, currentY, newX, newY);
                currentX = newX;
                currentY = newY;
                return moveEvent;
            });
        })
        // IDLE
        .thenCompose(event -> {
            display(event);
            return CompletableFuture.supplyAsync(() -> {
                // Robot emits IDLE event
                try {
                    Thread.sleep((long) (Math.random() * 1000));
                    return new Event(robotId, RobotEventType.IDLE, 0, 0, 0, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            });
        });
    }

    // Display method to simulate event handling (for debugging purposes)
    private void display(Event event) {
        System.out.println(event);
    }
}