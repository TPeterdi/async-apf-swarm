package async.apf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import async.apf.model.enums.Cardinal;
import async.apf.model.enums.RobotEventType;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.RobotEvent;

public class Robot {
    private final EventEmitter globalEventEmitter;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private boolean active = false;
    private int currentId;

    // Prevents COMPUTE cycle to start ahead of time
    private CompletableFuture lookFuture;

    // These get evaluated after each LOOK phase
    private RobotOrientation currentConfiguration;
    private ConfigurationOrientation targetPattern;

    private Cardinal nextMove = null;
    private int currentDelay;

    public Robot(EventEmitter globalEventEmitter) {
        this.globalEventEmitter = globalEventEmitter;
    }

    public synchronized void activate(int currentId, int currentDelay) {
        if (this.active) {
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.ACTIVE, this.currentId));
            return;
        }
        this.active = true;
        this.currentId = currentId;
        this.currentDelay = currentDelay;
        this.executorService.submit(this::cycleLoop);
    }

    public void supplyConfigurations(List<Coordinate> relativeConfiguration, List<Coordinate> targetPattern) {
        this.currentConfiguration = OrientationHelper.orientRobotAndConfiguration(relativeConfiguration);
        this.targetPattern        = OrientationHelper.orientConfiguration(targetPattern);
        this.lookFuture.complete(null);
    }

    private synchronized void cycleLoop() {
        try {
            // LOOK
            this.lookFuture = new CompletableFuture();
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.LOOK, this.currentId));
            this.lookFuture.get();
            awaitArtificialDelay();

            // COMPUTE
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.COMPUTE, this.currentId));
            computeNextMove();
            awaitArtificialDelay();

            // MOVE
            signalMovement();
            awaitArtificialDelay();

            resetState();
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.IDLE, this.currentId));
        }
        catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void awaitArtificialDelay() throws InterruptedException {
        if (this.currentDelay > 0) {
            Thread.sleep(this.currentDelay);
        }
    }

    private void signalMovement() throws AssertionError {
        if (this.nextMove == null) {
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.STAY_PUT, this.currentId));
        }
        else {
            Cardinal realMove = transformMoveBackToGlobalOrientation();

            switch (realMove) {
                case NORTH -> this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_NORTH, this.currentId));
                case EAST  -> this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_EAST,  this.currentId));
                case SOUTH -> this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_SOUTH, this.currentId));
                case WEST  -> this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.MOVE_WEST,  this.currentId));
                default -> throw new AssertionError();
            }
        }
    }

    private Cardinal transformMoveBackToGlobalOrientation() {
        Cardinal realMovement = this.nextMove;
        // rotate back to real north
        switch (currentConfiguration.getOrientation()) {
            case NORTH -> realMovement = this.nextMove;
            case EAST -> {
                switch (this.nextMove) {
                    case NORTH -> realMovement = Cardinal.EAST;
                    case EAST ->  realMovement = Cardinal.SOUTH;
                    case SOUTH -> realMovement = Cardinal.WEST;
                    case WEST ->  realMovement = Cardinal.NORTH;
                }
            }
            case SOUTH -> {
                switch (this.nextMove) {
                    case NORTH -> realMovement = Cardinal.SOUTH;
                    case EAST ->  realMovement = Cardinal.WEST;
                    case SOUTH -> realMovement = Cardinal.NORTH;
                    case WEST ->  realMovement = Cardinal.EAST;
                }
            }
            case WEST -> {
                switch (this.nextMove) {
                    case NORTH -> realMovement = Cardinal.WEST;
                    case EAST ->  realMovement = Cardinal.NORTH;
                    case SOUTH -> realMovement = Cardinal.EAST;
                    case WEST ->  realMovement = Cardinal.SOUTH;
                }
            }
            
        }
        // flip back
        if (currentConfiguration.isXMirrored()) {
            if (realMovement == Cardinal.EAST)
                realMovement = Cardinal.WEST;
            else if (realMovement == Cardinal.WEST)
                realMovement = Cardinal.EAST;
        }
        return realMovement;
    }

    private void resetState() {
        this.nextMove = null;
        this.currentConfiguration = null;
        this.active = false;
    }

    private void computeNextMove() {
        if (checkForPhaseI()) {
            doPhaseI();
        }
        else if (checkForPhaseII()) {
            doPhaseII();
        }
        else if (checkForPhaseIII()) {
            doPhaseIII();
        }
        else if (checkForPhaseIV()) {
            doPhaseIV();
        }
        else if (checkForPhaseV()) {
            doPhaseV();
        }
        else if (checkForPhaseVI()) {
            doPhaseVI();
        }
        else if (checkForPhaseVII()) {
            doPhaseVII();
        }
        else {
            this.globalEventEmitter.emitEvent(new RobotEvent(RobotEventType.PATTERN_COMPLETE, this.currentId));
        }
    }

    // #region CONDITION CHECKS
    // C = C_target
    private boolean checkC0() {
        boolean result = currentConfiguration.equals(targetPattern);
        return result;
    }

    // C' = C'_target
    private boolean checkC1() {
        boolean result = currentConfiguration.primeEquals(targetPattern);
        return result;
    }

    // C'' = C''_target
    private boolean checkC2() {
        boolean result = currentConfiguration.primePrimeEquals(targetPattern);
        return result;
    }

    // x-coordinate of the tail = x-coordinate of t_target
    private boolean checkC3() {
        boolean result = currentConfiguration.getTailPosition().getX() == targetPattern.getTailPosition().getX();
        return result;
    }

    // There is neither any robot except the tail nor any target positions
    // on or above H_t, where H_t is the horizontal line containing the tail
    private boolean checkC4() {
        Coordinate tailCoordinate = currentConfiguration.getTailPosition();
        int yHt = tailCoordinate.getY();

        // Look for other robots
        for (Coordinate robotCoordinate : currentConfiguration.getCoordinates()) {
            if (robotCoordinate.getY() < yHt ||
                robotCoordinate.equals(tailCoordinate))
                continue;

            return false;
        }
        // Look for target positions
        for (Coordinate targetCoordinate : targetPattern.getCoordinates()) {
            if (targetCoordinate.getY() < yHt)
                continue;

            return false;
        }

        return true;
    }

    // y-coordinate of the tail is odd
    private boolean checkC5() {
        boolean result = currentConfiguration.getTailPosition().getY() % 2 == 1;
        return result;
    }

    // SER of C is not a square
    private boolean checkC6() {
        boolean result = currentConfiguration.getWidth() != currentConfiguration.getHeight();
        return result;
    }

    // There is neither any robot except the tail nor any target positions
    // on or at the right of V_t, where V_t is the vertical line containing the tail
    private boolean checkC7() {
        Coordinate tailCoordinate = currentConfiguration.getTailPosition();
        int xVt = tailCoordinate.getX();

        // Look for other robots
        for (Coordinate robotCoordinate : currentConfiguration.getCoordinates()) {
            if (robotCoordinate.getX() < xVt ||
                robotCoordinate.equals(tailCoordinate))
                continue;

            return false;
        }
        // Look for target positions
        for (Coordinate targetCoordinate : targetPattern.getCoordinates()) {
            if (targetCoordinate.getX() < xVt)
                continue;

            return false;
        }

        return true;
    }

    // The head is at origin
    private boolean checkC8() {
        boolean result =
            currentConfiguration.getHeadPosition().getX() == 0
            && currentConfiguration.getHeadPosition().getY() == 0;
        return result;
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
        boolean result = !OrientationHelper.isSymmetric(copy);
        return result;
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
    // #endregion Initialization

    // #region PHASE LOGIC
    ///////////////////////// PHASE I /////////////////////////
    private boolean checkForPhaseI() {
        return
        !(
            checkC4() &&
            checkC5() &&
            checkC6()
        )
        &&
        !(
            checkC1() &&
            checkC3()
        );
    }
    private void doPhaseI() {
        // TAIL moves up
        if (currentConfiguration.getTailPosition().equals(currentConfiguration.getSelfPosition())) {
            this.nextMove = Cardinal.NORTH;
        }
    }

    ///////////////////////// PHASE II /////////////////////////
    private boolean checkForPhaseII() {
        return 
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
        );
    }
    private void doPhaseII() {
        // HEAD moves left
        if (currentConfiguration.getHeadPosition().equals(currentConfiguration.getSelfPosition())) {
            this.nextMove = Cardinal.WEST;
        }
    }

    ///////////////////////// PHASE III /////////////////////////
    private boolean checkForPhaseIII() {
        return
        checkC4() &&
        checkC5() &&
        checkC6() &&
        checkC8() &&
        !checkC2() &&
        !checkC7();
    }
    private void doPhaseIII() {
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

    ///////////////////////// PHASE IV /////////////////////////
    private boolean checkForPhaseIV() {
        return
        checkC4() &&
        checkC5() &&
        checkC6() &&
        checkC7() &&
        checkC8() &&
        !checkC2();
    }
    private void doPhaseIV() {
        Coordinate selfPosition = currentConfiguration.getSelfPosition();

        // only continue if this is an inner robot
        if (selfPosition.equals(currentConfiguration.getHeadPosition()) ||
            selfPosition.equals(currentConfiguration.getTailPosition())) {
            return;
        }

        int index = findOrderedIndexOfInnerRobot(selfPosition);

        Coordinate ri = selfPosition;
        Coordinate ti = targetPattern.getCoordinates().get(index);

        // REARRANGE (Algorithm 2)
        if (isTiLeftOfRi(ti, ri))
            calculateMoveTowardsTargetToTheLeft(ri, ti);
        else
            calculateMoveTowardsTargetToTheRight(ri, ti);
    }

    ///////////////////////// PHASE V /////////////////////////
    private boolean checkForPhaseV() {
        return
        checkC2() &&
        checkC4() &&
        checkC5() &&
        checkC6() &&
        checkC8() &&
        !checkC3();
    }
    private void doPhaseV() {
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

    ///////////////////////// PHASE VI /////////////////////////
    private boolean checkForPhaseVI() {
        return
        !checkC1() &&
        checkC2() &&
        checkC3() &&
        checkC4() &&
        checkC5() &&
        checkC6();
    }
    private void doPhaseVI() {
        // HEAD moves horizontally to reach h_target
        Coordinate headPosition = currentConfiguration.getHeadPosition();
        if (headPosition.equals(currentConfiguration.getSelfPosition()) &&
                !headPosition.equals(targetPattern.getHeadPosition())) {
            this.nextMove = Cardinal.EAST;
        }
    }

    ///////////////////////// PHASE VII /////////////////////////
    private boolean checkForPhaseVII() {
        return
        !checkC0() &&
        checkC1() &&
        checkC3();
    }
    private void doPhaseVII() {
        // TAIL moves vertically to reach t_target
        Coordinate tailPosition = currentConfiguration.getTailPosition();
        if (tailPosition.equals(currentConfiguration.getSelfPosition()) &&
            !tailPosition.equals(targetPattern.getTailPosition())) {
            if (targetPattern.getTailPosition().getY() > tailPosition.getY()) {
                this.nextMove = Cardinal.NORTH;
            }
            else {
                this.nextMove = Cardinal.SOUTH;
            }
        }
    }

    // #endregion PHASE LOGIC
    
    // #region HELPERS
    private void calculateMoveTowardsTargetToTheLeft(Coordinate ri, Coordinate ti) {
        if (!noOtherRobotInSubPathFromRiToTi(ri, ti))
            return;

        if (isRiAndTiOnSameLine(ri, ti)) {
            // ri moves towards ti through the vertical (or horizontal) line joining ri and ti
            if (ri.getX() == ti.getX()) {
                // same vertical line
                this.nextMove = Cardinal.SOUTH;
            }
            else {
                // same horizontal line
                this.nextMove = Cardinal.WEST;
            }
        }
        else {
            Coordinate adj = new Coordinate(ri.getX(), ri.getY() - 1);
            if (isTiLeftOfRi(adj, ri)) {
                // ri moves to its left adjacent node on P
                findMoveTowardsLeftAdjacentNode(ri);
            }
            else {
                // ri moves downwards
                this.nextMove = Cardinal.SOUTH;
            }
        }
    }
    
    private void calculateMoveTowardsTargetToTheRight(Coordinate ri, Coordinate ti) {
        if (!noInnerRobotWithTjAtLeftOfRj())
            return;

        if (!noOtherRobotInSubPathFromRiToTi(ri, ti))
            return;

        if (isRiAndTiOnSameLine(ri, ti)) {
            // ri moves towards ti through the vertical (or horizontal) line joining ri and ti
            if (ri.getX() == ti.getX()) {
                // same vertical line
                this.nextMove = Cardinal.NORTH;
            }
            else {
                // same horizontal line
                this.nextMove = Cardinal.EAST;
            }
        }
        else {
            Coordinate adj = new Coordinate(ri.getX(), ri.getY() + 1);
            if (isTiLeftOfRi(adj, ri)) {
                // ri moves upwards
                this.nextMove = Cardinal.NORTH;
            }
            else {
                findMoveTowardsRightAdjacentNode(ri);
            }
        }
    }

    private int findOrderedIndexOfInnerRobot(Coordinate selfPosition) {
        List<Coordinate> robotPositions = currentConfiguration.getCoordinates();
        for (int i = 1; i < robotPositions.size() - 1; i++) {
            if (robotPositions.get(i).equals(selfPosition)) {
                return i;
            }
        }
        // should never happen
        return -1;
    }

    private boolean isTiLeftOfRi(Coordinate ti, Coordinate ri) {
        int rx = ri.getX();
        int ry = ri.getY();
        int tx = ti.getX();
        int ty = ti.getY();

        if (ry > ty)
            return true;
            
        if (ry < ty)
            return false;

        // same y-coordinate
        
        return ry % 2 == 0
            ? tx < rx
            : rx < tx;
    }

    private boolean noOtherRobotInSubPathFromRiToTi(Coordinate ri, Coordinate ti) {
        int rIndex = OrientationHelper.coordinateToIndex(ri, currentConfiguration.getWidth()) + 1;
        int tIndex = OrientationHelper.coordinateToIndex(ti, currentConfiguration.getWidth()) - 1;
        int startIndex = Math.min(rIndex, tIndex);
        int endIndex   = Math.max(rIndex, tIndex);
        for (int i = startIndex; i <= endIndex; i++) {
            if (currentConfiguration.getBinaryRepresentation().get(endIndex)) {
                return false;
            }
        }
        return true;
    }

    private boolean isRiAndTiOnSameLine(Coordinate ri, Coordinate ti) {
        return ri.getX() == ti.getX() || ri.getY() == ti.getY();
    }

    private void findMoveTowardsLeftAdjacentNode(Coordinate ri) {
        if (ri.getX() == 0) {
            this.nextMove = Cardinal.SOUTH;
        }
        else if (ri.getX() - 1 == currentConfiguration.getWidth()) {
            this.nextMove = Cardinal.SOUTH;
        }
        else {
            if (ri.getY() % 2 == 0) {
                this.nextMove = Cardinal.WEST;
            }
            else {
                this.nextMove = Cardinal.EAST;
            }
        }
    }

    private boolean noInnerRobotWithTjAtLeftOfRj() {
        // Check if there is no inner robot rj such that tj is at the left of rj
        for (int j = 1; j < currentConfiguration.getCoordinates().size() - 1; j++) {
            Coordinate rj = currentConfiguration.getCoordinates().get(j);
            Coordinate tj = targetPattern.getCoordinates().get(j);
            if (isTiLeftOfRi(tj, rj)) {
                return false;
            }
        }
        return true;
    }

    private void findMoveTowardsRightAdjacentNode(Coordinate ri) {
        // ri moves to its right adjacent node on P
        if (ri.getX() == 0) {
            this.nextMove = Cardinal.NORTH;
        }
        else if (ri.getX() - 1 == currentConfiguration.getWidth()) {
            this.nextMove = Cardinal.NORTH;
        }
        else {
            if (ri.getY() % 2 == 0) {
                this.nextMove = Cardinal.EAST;
            }
            else {
                this.nextMove = Cardinal.WEST;
            }
        }
    }
    // #endregion HELPERS
}