package async.apf.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import async.apf.model.enums.Cardinal;
import async.apf.model.enums.RobotEventType;
import async.apf.model.events.RobotEvent;

public class Robot {
    private boolean working = false;
    private CountDownLatch lookLatch;
    private List<Coordinate> currentConfiguration;
    private Cardinal nextMove = null;

    public Robot() {}

    public CompletableFuture<RobotEvent> activate() {
        if (this.working) return CompletableFuture.completedFuture(new RobotEvent(RobotEventType.WORKING));

        this.working = true;
        this.lookLatch = new CountDownLatch(1);
        Cardinal orientation = null;
        // LOOK
        return CompletableFuture.supplyAsync(() -> {
            return new RobotEvent(RobotEventType.LOOK);
        })
        // COMPUTE
        .thenCompose(event -> {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        })
        // MOVE
        .thenCompose(event -> {
            return CompletableFuture.supplyAsync(() -> {
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
            });
        })
        // IDLE
        .thenCompose(event -> {
            return CompletableFuture.supplyAsync(() -> {
                this.nextMove = null;
                this.currentConfiguration = null;
                return new RobotEvent(RobotEventType.IDLE);
            });
        });
    }

    public void supplyConfiguration(List<Coordinate> configuration) {
        this.currentConfiguration = configuration;
        lookLatch.countDown();
    }
}