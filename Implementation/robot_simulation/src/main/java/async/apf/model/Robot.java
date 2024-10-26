package async.apf.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import async.apf.model.enums.Cardinal;
import async.apf.model.enums.RobotEventType;
import async.apf.model.events.RobotEvent;

public class Robot {
    private boolean active = false;

    // Prevents COMPUTE cycle to start ahead of time
    private CountDownLatch lookLatch;

    // These get evaluated after each LOOK phase
    private RobotOrientation currentConfiguration;
    private ConfigurationOrientation targetPattern;


    private Cardinal nextMove = null;

    public CompletableFuture<RobotEvent> activate() {
        if (this.active) return CompletableFuture.completedFuture(new RobotEvent(RobotEventType.ACTIVE));

        this.active = true;
        this.lookLatch = new CountDownLatch(1);
        // LOOK
        return CompletableFuture.supplyAsync(() -> {
            return new RobotEvent(RobotEventType.LOOK);
        })
        // COMPUTE
        .thenCompose(event ->
            CompletableFuture.supplyAsync(() -> {
                try {
                    lookLatch.await();

                    // TODO: Do actual computation
                    Thread.sleep((long) (Math.random() * 1000));
                    this.nextMove = Cardinal.EAST;

                    return new RobotEvent(RobotEventType.COMPUTE);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            })
        )
        // MOVE
        .thenCompose(event ->
            CompletableFuture.supplyAsync(() -> {
                switch (this.nextMove) {
                    case NORTH -> {
                        return new RobotEvent(RobotEventType.MOVE_NORTH);
                    }
                    case EAST -> {
                        return new RobotEvent(RobotEventType.MOVE_EAST);
                    }
                    case SOUTH -> {
                        return new RobotEvent(RobotEventType.MOVE_SOUTH);
                    }
                    case WEST -> {
                        return new RobotEvent(RobotEventType.MOVE_WEST);
                    }
                    default -> throw new AssertionError();
                }
            })
        )
        // IDLE
        .thenCompose(event -> 
            CompletableFuture.supplyAsync(() -> {
                this.nextMove = null;
                this.currentConfiguration = null;
                return new RobotEvent(RobotEventType.IDLE);
            })
        );
    }

    public void supplyConfigurations(List<Coordinate> relativeConfiguration, List<Coordinate> targetPattern) {
        this.currentConfiguration = OrientationHelper.orientRobotAndConfiguration(relativeConfiguration);
        this.targetPattern        = OrientationHelper.orientPattern(targetPattern);
        lookLatch.countDown();
    }
}