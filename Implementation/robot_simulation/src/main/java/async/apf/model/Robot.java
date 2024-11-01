package async.apf.model;

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
        this.targetPattern.changeOrientation(currentConfiguration.getOrientation(), currentConfiguration.isXMirrored());
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
        this.c0 = null;
        this.c1 = null;
        this.c2 = null;
        this.c3 = null;
        this.c4 = null;
        this.c5 = null;
        this.c6 = null;
        this.c7 = null;
        this.c8 = null;
        this.c9 = null;
        this.c10 = null;
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

    private Boolean c0;
    private void  checkC0() {
        c0 = currentConfiguration.equals(targetPattern);
    }
    /**
     * C = C_target
     */
    private boolean getC0() {
        if (c0 == null) checkC0();
        return c0;
    }

    private Boolean c1;
    // C' = C'_target
    private void checkC1() {
        c1 = currentConfiguration.primeEquals(targetPattern);
    }
    /**
     * C' = C'_target
     */
    private boolean getC1() {
        if (c1 == null) checkC1();
        return c1;
    }

    private Boolean c2;
    // C'' = C''_target
    private void checkC2() {
        c2 = currentConfiguration.primePrimeEquals(targetPattern);
    }
    /**
     * C'' = C''_target
     */
    private boolean getC2() {
        if (c2 == null) checkC2();
        return c2;
    }

    private Boolean c3;
    // x-coordinate of the tail = x-coordinate of t_target
    private void checkC3() {
        c3 = currentConfiguration.getTailPosition().getX() == targetPattern.getTailPosition().getX();
    }
    /**
     * x-coordinate of the tail = x-coordinate of t_target
     */
    private boolean getC3() {
        if (c3 == null) checkC3();
        return c3;
    }

    private Boolean c4;
    // There is neither any robot except the tail nor any target positions
    // on or above H_t, where H_t is the horizontal line containing the tail
    private void checkC4() {
        Coordinate tailCoordinate = currentConfiguration.getTailPosition();
        int yHt = tailCoordinate.getY();

        // Look for other robots
        for (Coordinate robotCoordinate : currentConfiguration.getCoordinates()) {
            if (robotCoordinate.getY() < yHt ||
                robotCoordinate.equals(tailCoordinate))
                continue;

            c4 = false;
            return;
        }
        // Look for target positions
        for (Coordinate targetCoordinate : targetPattern.getCoordinates()) {
            if (targetCoordinate.getY() < yHt)
                continue;

            c4 = false;
            return;
        }

        c4 = true;
    }
    /**
     * There is neither any robot except the tail nor any target positions
     * on or above H_t, where H_t is the horizontal line containing the tail
     */
    private boolean getC4() {
        if (c4 == null) checkC4();
        return c4;
    }

    private Boolean c5;
    // y-coordinate of the tail is odd
    private void checkC5() {
        c5 = currentConfiguration.getTailPosition().getY() % 2 == 1;
    }
    /**
     * y-coordinate of the tail is odd
     */
    private boolean getC5() {
        if (c5 == null) checkC5();
        return c5;
    }

    private Boolean c6;
    // SER of C is not a square
    private void checkC6() {
        c6 = currentConfiguration.getWidth() != currentConfiguration.getHeight();
    }
    /**
     * SER of C is not a square
     */
    private boolean getC6() {
        if (c6 == null) checkC6();
        return c6;
    }

    private Boolean c7;
    // There is neither any robot except the tail nor any target positions
    // on or at the right of V_t, where V_t is the vertical line containing the tail
    private void checkC7() {
        Coordinate tailCoordinate = currentConfiguration.getTailPosition();
        int xVt = tailCoordinate.getX();

        // Look for other robots
        for (Coordinate robotCoordinate : currentConfiguration.getCoordinates()) {
            if (robotCoordinate.getX() < xVt ||
                robotCoordinate.equals(tailCoordinate))
                continue;

            c7 = false;
            return;
        }
        // Look for target positions
        for (Coordinate targetCoordinate : targetPattern.getCoordinates()) {
            if (targetCoordinate.getX() < xVt)
                continue;

            c7 = false;
            return;
        }

        c7 = true;
    }
    /**
     * There is neither any robot except the tail nor any target positions
     * on or at the right of V_t, where V_t is the vertical line containing the tail
     */
    private boolean getC7() {
        if (c7 == null) checkC7();
        return c7;
    }

    private Boolean c8;
    // The head is at origin
    private void checkC8() {
        c8 = currentConfiguration.getHeadPosition().getX() == 0 &&
             currentConfiguration.getHeadPosition().getY() == 0;
    }
    /**
     * The head is at origin
     */
    private boolean getC8() {
        if (c8 == null) checkC8();
        return c8;
    }

    private Boolean c9;
    // If the tail and the head are relocated respectively at C and A, then
    // the new configuration remains asymmetric
    private void checkC9() {
        List<Coordinate> copy = OrientationHelper.copyCoordinates(currentConfiguration.getCoordinates());
        copy.getFirst().setX(0);
        copy.getFirst().setY(0);
        copy.getLast().setX(currentConfiguration.getWidth() - 1);
        copy.getLast().setY(currentConfiguration.getHeight() - 1);
        c9 = !OrientationHelper.isSymmetric(copy);
    }
    /**
     * If the tail and the head are relocated respectively at C and A, then
     * the new configuration remains asymmetric
     */
    private boolean getC9() {
        if (c9 == null) checkC9();
        return c9;
    }

    private Boolean c10;
    // C′ has a symmetry with respect to a vertical line
    private void checkC10() {
        List<Coordinate> primeCoordinates = currentConfiguration.getCoordinates()
            .subList(0, currentConfiguration.getCoordinates().size() - 1);
        int primeMaxX = primeCoordinates
            .stream()
            .mapToInt(Coordinate::getX)
            .max()
            .orElseThrow();
        for (Coordinate position : primeCoordinates) {
            Coordinate mirrored = new Coordinate(primeMaxX - position.getX(), position.getY());
            if (!currentConfiguration.getCoordinates().contains(mirrored)) {
                c10 = false;
                return;
            }
        }
        c10 = true;
    }
    /**
     * C′ has a symmetry with respect to a vertical line
     */
    private boolean getC10() {
        if (c10 == null) checkC10();
        return c10;
    }
    // #endregion Initialization

    // #region PHASE LOGIC
    ///////////////////////// PHASE I /////////////////////////
    private boolean checkForPhaseI() {
        return
        !(
            getC4() &&
            getC5() &&
            getC6()
        )
        &&
        !(
            getC1() &&
            getC3()
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
            getC4() &&
            getC5() &&
            getC6() &&
            !getC8()
        )
        &&
        (
            (
                getC2() &&
                !getC3()
            )
            ||
            !getC2()
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
        getC4() &&
        getC5() &&
        getC6() &&
        getC8() &&
        !getC2() &&
        !getC7();
    }
    private void doPhaseIII() {
        // The aim of this phase is to make C7 true
        if (currentConfiguration.getTailPosition().equals(currentConfiguration.getSelfPosition())) {
            if (getC10()) {
                // TAIL moves left or upwards in accordance with
                // m > n + 1 or m = n + 1
                if (currentConfiguration.getHeight() > currentConfiguration.getWidth() + 1) {
                    this.nextMove = Cardinal.EAST;
                }
                else {
                    this.nextMove = Cardinal.NORTH;
                }
            }
            else {
                if (getC9()) {
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
        getC4() &&
        getC5() &&
        getC6() &&
        getC7() &&
        getC8() &&
        !getC2();
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
        getC2() &&
        getC4() &&
        getC5() &&
        getC6() &&
        getC8() &&
        !getC3();
    }
    private void doPhaseV() {
        // TAIL moves horizontally to make C3 true
        if (currentConfiguration.getTailPosition().equals(currentConfiguration.getSelfPosition())) {
            if (getC10()) {
                int maxPrimeX = Integer.MIN_VALUE;
                
                // Iterate through the list except the last element
                for (int i = 0; i < currentConfiguration.getCoordinates().size() - 1; i++) {
                    Coordinate p = currentConfiguration.getCoordinates().get(i);
                    maxPrimeX = Math.max(maxPrimeX, p.getX());
                }
                int tPrimeX = targetPattern.getTailPosition().getX();
                int tX = currentConfiguration.getTailPosition().getX();
                int cPrimePrimeX = maxPrimeX;
                int eX = maxPrimeX / 2;
                
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
        !getC1() &&
        getC2() &&
        getC3() &&
        getC4() &&
        getC5() &&
        getC6();
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
        !getC0() &&
        getC1() &&
        getC3();
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