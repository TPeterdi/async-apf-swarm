package async.apf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import async.apf.model.enums.Cardinal;
import async.apf.model.enums.RobotEventType;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.RobotEvent;

public class Robot {
    private final EventEmitter globalEventEmitter;
    private Thread cycleThread;

    private boolean active = false;
    private int currentId;

    // Prevents COMPUTE cycle to start ahead of time
    private CountDownLatch lookLatch;

    // These get evaluated after each LOOK phase
    private RobotOrientation currentConfiguration;
    private ConfigurationOrientation targetPattern;

    private Cardinal nextMove = null;

    public Robot(EventEmitter globalEventEmitter) {
        this.globalEventEmitter = globalEventEmitter;
        this.resetCycle();
    }

    public void activate(int currentId) {
        if (this.active) {
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.ACTIVE, this.currentId));
            return;
        }
        this.active = true;
        this.currentId = currentId;
        this.cycleThread.start();
    }

    public void supplyConfigurations(List<Coordinate> relativeConfiguration, List<Coordinate> targetPattern) {
        this.currentConfiguration = OrientationHelper.orientRobotAndConfiguration(relativeConfiguration);
        this.targetPattern        = OrientationHelper.orientConfiguration(targetPattern);
        lookLatch.countDown();
    }

    private void resetCycle() {
        this.cycleThread = new Thread(() -> {
            this.lookLatch = new CountDownLatch(1);
    
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.LOOK, this.currentId));
    
            try {
                lookLatch.await();
            } catch (Exception e) {
                return;
            }
    
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.COMPUTE, this.currentId));
    
            computeNextMove();
            if (this.nextMove == null) {
                this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.STAY_PUT, this.currentId));
            }
            else {
                switch (this.nextMove) {
                    case NORTH -> {
                        this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_NORTH, this.currentId));
                    }
                    case EAST -> {
                        this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_EAST, this.currentId));
                    }
                    case SOUTH -> {
                        this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_SOUTH, this.currentId));
                    }
                    case WEST -> {
                        this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_WEST, this.currentId));
                    }
                    default -> throw new AssertionError();
                }
            }
    
            this.nextMove = null;
            this.currentConfiguration = null;
            this.active = false;
            this.resetCycle();
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.IDLE, this.currentId));
        });
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
            // TAIL moves up
            if (currentConfiguration.getTailPosition().equals(currentConfiguration.getSelfPosition())) {
                this.nextMove = Cardinal.NORTH;
            }
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
            // HEAD moves left
            if (currentConfiguration.getHeadPosition().equals(currentConfiguration.getSelfPosition())) {
                this.nextMove = Cardinal.WEST;
            }
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
            // The aim of this phase is to make C7 true
            if (currentConfiguration.getTailPosition().equals(currentConfiguration.getSelfPosition())) {
                if (checkC10()) {
                    // TAIL moves left or upwards in accordance with
                    // m > n + 1 or m = n + 1
                    if (currentConfiguration.getHeight() > currentConfiguration.getWidth() + 1) {
                        this.nextMove = Cardinal.WEST;
                    }
                    else {
                        this.nextMove = Cardinal.NORTH;
                    }
                }
                else {
                    if (checkC9()) {
                        // TAIL moves right or upwards in accordance with
                        // m > n + 1 or m = n + 1
                        // (dimension of the current SER is m × n with m ≥ n)
                        if (currentConfiguration.getHeight() > currentConfiguration.getWidth() + 1) {
                            this.nextMove = Cardinal.EAST;
                        }
                        else {
                            this.nextMove = Cardinal.NORTH;
                        }
                    }
                    else {
                        // TAIL moves up
                        this.nextMove = Cardinal.NORTH;
                    }
                }
            }
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
            rearrange();
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
            // TAIL moves horizontally to make C3 true
            if (currentConfiguration.getTailPosition().equals(currentConfiguration.getSelfPosition())) {
                if (checkC10()) {
                    int maxX = Integer.MIN_VALUE;

                    // Iterate through the list except the last element
                    for (int i = 0; i < currentConfiguration.getCoordinates().size() - 1; i++) {
                        Coordinate p = currentConfiguration.getCoordinates().get(i);
                        maxX = Math.max(maxX, p.getX());
                    }
                    int tPrimeX = targetPattern.getTailPosition().getX();
                    int tX = currentConfiguration.getTailPosition().getX();
                    int cPrimePrimeX = maxX;
                    int eX = maxX / 2;

                    if ((tX > cPrimePrimeX && tPrimeX > cPrimePrimeX) ||
                        (tX <= eX && tPrimeX <= eX)) {
                        if (tX > tPrimeX) {
                            this.nextMove = Cardinal.WEST;
                        }
                        else {
                            this.nextMove = Cardinal.EAST;
                        }
                    }
                    else {
                        this.nextMove = Cardinal.WEST;
                    }
                }
                else {
                    // TAIL moves horizontally towards t_target's x-coordinate.
                    if (targetPattern.getTailPosition().getX() < currentConfiguration.getTailPosition().getX()) {
                        this.nextMove = Cardinal.WEST;
                    }
                    else {
                        this.nextMove = Cardinal.EAST;
                    }
                }
            }
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
            // HEAD moves horizontally to reach h_target
            Coordinate headPosition = currentConfiguration.getHeadPosition();
            if (headPosition.equals(currentConfiguration.getSelfPosition()) &&
                !headPosition.equals(targetPattern.getHeadPosition())) {
                this.nextMove = Cardinal.EAST;
            }
        }
        else if (
            !checkC0() &&
            checkC1() &&
            checkC3()
        )
        // PHASE VII
        {
            // TAIL moves vertically to reach t_target
            Coordinate tailPosition = currentConfiguration.getTailPosition();
            if (tailPosition.equals(currentConfiguration.getSelfPosition()) &&
                !tailPosition.equals(targetPattern.getTailPosition())) {
                this.nextMove = Cardinal.SOUTH;
            }
        }
    }

    private void rearrange() {
        // TODO: order target and robot positions
        // TODO: implement REARRANGE pseudo-code (Algorithm 2)
    }
}