package async.apf.model;

import java.util.ArrayList;
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
        // SIGNAL COMPUTE
        .thenCompose(event ->
            CompletableFuture.supplyAsync(() -> {
                try {
                    lookLatch.await();

                    return new RobotEvent(RobotEventType.COMPUTE);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            })
        )
        // CARRY OUT COMPUTE AND SIGNAL MOVE
        .thenCompose(event ->
            CompletableFuture.supplyAsync(() -> {
                computeNextMove();
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
        this.targetPattern        = OrientationHelper.orientConfiguration(targetPattern);
        lookLatch.countDown();
    }

    // C = C_target
    private boolean checkC0() {
        return currentConfiguration.equals(targetPattern);
    }

    // C' = C'_target
    private boolean checkC1() {
        return currentConfiguration.primeEquals(targetPattern);
    }

    // C'' = C''_target
    private boolean checkC2() {
        return currentConfiguration.primePrimeEquals(targetPattern);
    }

    // x-coordinate of the tail = x-coordinate of t_target
    private boolean checkC3() {
        return currentConfiguration.getTailPosition().getX() == targetPattern.getTailPosition().getX();
    }

    // There is neither any robot except the tail nor any target positions
    // on or above H_t, where H_t is the horizontal line containing the tail
    private boolean checkC4() {
        Coordinate tailCoordinate = currentConfiguration.getTailPosition();
        int yHt = tailCoordinate.getY();

        // Check for other robots
        for (int y = yHt; y < currentConfiguration.getHeight(); y++) {
            for (int x = 0; x < currentConfiguration.getWidth(); x++) {
                if (x == tailCoordinate.getX() && y == yHt) continue;
                if (currentConfiguration.isCoordinateMarked(x, y)) {
                    return false;
                }
            }
        }
        // Check for target positions
        for (int y = yHt; y < targetPattern.getHeight(); y++) {
            for (int x = 0; x < targetPattern.getWidth(); x++) {
                if (targetPattern.isCoordinateMarked(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    // y-coordinate of the tail is odd
    private boolean checkC5() {
        return currentConfiguration.getTailPosition().getY() % 2 == 1;
    }

    // SER of C is not a square
    private boolean checkC6() {
        return currentConfiguration.getWidth() != currentConfiguration.getHeight();
    }

    // There is neither any robot except the tail nor any target positions
    // on or at the right of V_t, where V_t is the vertical line containing the tail
    private boolean checkC7() {
        Coordinate tailCoordinate = currentConfiguration.getTailPosition();
        int xVt = tailCoordinate.getX();

        // Check for other robots
        for (int x = xVt; x < currentConfiguration.getWidth(); x++) {
            for (int y = 0; y < currentConfiguration.getHeight(); y++) {
                if (x == xVt && y == tailCoordinate.getY()) continue;
                if (currentConfiguration.isCoordinateMarked(x, y)) {
                    return false;
                }
            }
        }
        // Check for target positions
        for (int x = xVt; x < targetPattern.getWidth(); x++) {
            for (int y = 0; y < targetPattern.getHeight(); y++) {
                if (targetPattern.isCoordinateMarked(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

    // The head is at origin
    private boolean checkC8() {
        return currentConfiguration.getHeadPosition().getX() == 0
            && currentConfiguration.getHeadPosition().getY() == 0;
    }

    // If the tail and the head are relocated respectively at C and A, then
    // the new configuration remains asymmetric
    private boolean checkC9() {
        List<Coordinate> copy = new ArrayList<>();
        for (Coordinate pos : currentConfiguration.getCoordinates()) {
            copy.add(new Coordinate(pos.getX(), pos.getY()));
        }
        copy.getFirst().setX(0);
        copy.getFirst().setY(0);
        copy.getLast().setX(currentConfiguration.getWidth() - 1);
        copy.getLast().setY(currentConfiguration.getHeight() - 1);
        return !OrientationHelper.isSymmetric(copy);
    }

    // C′ has a symmetry with respect to a vertical line
    private boolean checkC10() {
        List<Coordinate> primeCoordinates = currentConfiguration.getCoordinates()
            .subList(0, currentConfiguration.getCoordinates().size() - 1);
        int primeWidth = primeCoordinates
            .stream()
            .mapToInt(Coordinate::getX)
            .max()
            .orElseThrow();
        for (int idx = 0; idx < primeWidth; idx++) {
            Coordinate position = currentConfiguration.getCoordinates().get(idx);
            if (!currentConfiguration.isCoordinateMarked(primeWidth - position.getX() -1, position.getY())) {
                return false;
            }
        }
        return true;
    }

    private void computeNextMove() {
        if (
            !(
                checkC4() &&
                checkC5() &&
                checkC6()
            )
            &&
            !(
                checkC1() &&
                checkC3()
            )
        )
        // PHASE I
        {

        }
        else if (
            (
                checkC4() &&
                checkC5() &&
                checkC6() &&
                !checkC8()
            )
            &&
            (
                (
                    checkC2() &&
                    checkC3()
                )
                ||
                !checkC2()
            )
        )
        // PHASE II
        {

        }
        else if (
            checkC4() &&
            checkC5() &&
            checkC6() &&
            checkC8() &&
            !checkC2() &&
            !checkC7()
        )
        // PHASE III
        {
            
        }
        else if (
            checkC4() &&
            checkC5() &&
            checkC6() &&
            checkC7() &&
            checkC8() &&
            !checkC2()
        )
        // PHASE IV
        {
            
        }
        else if (
            checkC2() &&
            checkC4() &&
            checkC5() &&
            checkC6() &&
            checkC8() &&
            !checkC3()
        )
        // PHASE V
        {
            
        }
        else if (
            !checkC1() &&
            checkC2() &&
            checkC3() &&
            checkC4() &&
            checkC5() &&
            checkC6()
        )
        // PHASE VI
        {
            
        }
        else if (
            !checkC0() &&
            checkC1() &&
            checkC3()
        )
        // PHASE VII
        {
            
        }
    }
}