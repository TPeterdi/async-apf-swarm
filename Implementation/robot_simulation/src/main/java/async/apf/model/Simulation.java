package async.apf.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import async.apf.interfaces.IEvent;
import async.apf.interfaces.IEventListener;
import async.apf.model.enums.RobotEventType;
import async.apf.model.enums.SimulationEventType;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.RobotEvent;
import async.apf.model.events.SimulationEvent;
import async.apf.model.exceptions.InvalidInputException;

public class Simulation implements IEventListener {
    private final AsyncScheduler scheduler;
    private final EventEmitter globalEventEmitter;

    private final List<Coordinate> currentConfiguration;
    private final List<Coordinate> targetPattern;

    private final List<Robot> robots;

    private boolean hasBegun = false;
    private volatile boolean isPaused = false;
    private volatile boolean completed = false;

    private final Thread simulationThread;

    private int delay;

    private final SimulationStatistics statistics;

    // Constructor to initialize the simulation with starting configuration, target pattern, and robots
    public Simulation(EventEmitter globalEventEmitter, List<Coordinate> startingConfiguration, List<Coordinate> targetPattern) throws InvalidInputException {
        this.globalEventEmitter = globalEventEmitter;
        this.currentConfiguration = startingConfiguration;
        this.targetPattern = targetPattern;

        int robotCount = startingConfiguration.size();

        if (robotCount != targetPattern.size()) {
            throw new InvalidInputException();
        }
        
        this.scheduler = new AsyncScheduler(robotCount);
        List<SER> SERs = getSER();
        this.statistics = new SimulationStatistics(
            robotCount,
            Math.max(SERs.get(0).getWidth(),  SERs.get(1).getWidth() ),
            Math.max(SERs.get(0).getHeight(), SERs.get(1).getHeight())
        );

        this.robots = new ArrayList<>();
        for (int i = 0; i < robotCount; i++) {
            this.robots.add(new Robot(globalEventEmitter));
        }

        this.simulationThread = new Thread(() -> {
            Random random = new Random();
            while (!this.completed) {
                synchronized (this) {
                    if (this.isPaused) {
                        try {
                            this.wait(); // Wait until notified
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                int randomIndex = this.scheduler.pickNext();
                Robot pickedRobot = this.robots.get(randomIndex);
                try {
                    if (this.delay > 0) {
                        int desync = random.nextInt(20);
                        Thread.sleep(desync);
                    }
                    pickedRobot.activate(randomIndex, this.delay);
                    this.statistics.incrementActivationCounter(randomIndex);
                }
                catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
            globalEventEmitter.emitEvent(new SimulationEvent(SimulationEventType.SIMULATION_END));
        });
    }

    private List<SER> getSER() {
        int minSX = Integer.MAX_VALUE;
        int minSY = Integer.MAX_VALUE;
        int maxSX = Integer.MIN_VALUE;
        int maxSY = Integer.MIN_VALUE;
        int minTX = Integer.MAX_VALUE;
        int minTY = Integer.MAX_VALUE;
        int maxTX = Integer.MIN_VALUE;
        int maxTY = Integer.MIN_VALUE;
        // Iterate over the points to find the min and max x, y values
        for (int i = 0; i < this.currentConfiguration.size(); i++) {
            int sx = currentConfiguration.get(i).getX();
            int sy = currentConfiguration.get(i).getY();
            minSX = Math.min(minSX, sx);
            minSY = Math.min(minSY, sy);
            maxSX = Math.max(maxSX, sx);
            maxSY = Math.max(maxSY, sy);
            int tx = targetPattern.get(i).getX();
            int ty = targetPattern.get(i).getY();
            minTX = Math.min(minTX, tx);
            minTY = Math.min(minTY, ty);
            maxTX = Math.max(maxTX, tx);
            maxTY = Math.max(maxTY, ty);
        }

        List<SER> result = new ArrayList<>();
        result.add(new SER(minSX, minSY, maxSX, maxSY));
        result.add(new SER(minTX, minTY, maxTX, maxTY));
        return result;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void begin() {
        if (this.hasBegun) return;

        this.hasBegun = true;
        this.isPaused = false;
        this.completed = false;
        globalEventEmitter.emitEvent(new SimulationEvent(SimulationEventType.SIMULATION_START));
        this.statistics.setStartTime(Instant.now());
        this.simulationThread.start();
    }

    public synchronized void resume() {
        this.isPaused = false;
        this.notify(); // Notify the thread to resume
    }

    public void pause() {
        this.isPaused = true;
    }

    public boolean hasBegun() {
        return hasBegun;
    }

    public boolean isRunning() {
        return hasBegun && !isPaused;
    }

    public boolean isPaused() {
        return hasBegun && isPaused;
    }

    private List<Coordinate> translateConfigurationToRobotsCoordinate(Coordinate robotCoordinate) {
        List<Coordinate> translatedCoordinates = new ArrayList<>();
        for (Coordinate coordinate : this.currentConfiguration) {
            translatedCoordinates.add(coordinate.translate(robotCoordinate));
        }
        return translatedCoordinates;
    }

    private void checkForCollisions() {
        Set<Coordinate> seenPoints = new HashSet<>();
        Set<Coordinate> collisions = new HashSet<>();
        
        for (Coordinate coordinate : this.currentConfiguration)
            if (!seenPoints.add(coordinate))
                // If add returns false, the current coordinate is a duplicate!
                collisions.add(coordinate);
        
        if (!collisions.isEmpty())
            for (Coordinate coordinate : collisions)
                System.err.println("Collision at " + coordinate.toString() + "!");
    }

    @Override
    public void onEvent(IEvent event) {
        if (!(event instanceof RobotEvent robotEvent)) return;

        RobotEventType eventType = robotEvent.getEventType();
        int index = robotEvent.getId();
        int phase = robotEvent.getPhase();

        Robot pickedRobot = robots.get(index);
        Coordinate currentCoordinate = currentConfiguration.get(index);
        int currentX = currentCoordinate.getX();
        int currentY = currentCoordinate.getY();

        switch (eventType) {
            case ACTIVE             -> handleActive();
            case STAY_PUT           -> handleStayPut();
            case LOOK               -> handleLookEvent(index, phase, currentX, currentY, pickedRobot);
            case COMPUTE            -> emitRobotEvent(index, SimulationEventType.ROBOT_COMPUTING, phase, currentX, currentY);
            case MOVE_NORTH         -> handleMoveEvent(index, phase, currentX, currentY, 0, 1);
            case MOVE_EAST          -> handleMoveEvent(index, phase, currentX, currentY, 1, 0);
            case MOVE_SOUTH         -> handleMoveEvent(index, phase, currentX, currentY, 0, -1);
            case MOVE_WEST          -> handleMoveEvent(index, phase, currentX, currentY, -1, 0);
            case IDLE               -> emitRobotEvent(index, SimulationEventType.ROBOT_IDLE, phase, currentX, currentY);
            case PATTERN_COMPLETE   -> endSimulation();
            default -> throw new IllegalArgumentException("Unexpected value: " + eventType);
        }
    }
    
    private void handleActive() {
        // Handle ACTIVE event if any specific logic is needed
    }
    
    private void handleStayPut() {
        // Handle STAY_PUT event if any specific logic is needed
    }
    
    private void handleLookEvent(int index, int phase, int x, int y, Robot robot) {
        statistics.incrementCycleCounter(index);
        emitRobotEvent(index, SimulationEventType.ROBOT_LOOKING, phase, x, y);
        Coordinate robotLocation = currentConfiguration.get(index);
        robot.supplyConfigurations(translateConfigurationToRobotsCoordinate(robotLocation), this.targetPattern);
    }
    
    private void handleMoveEvent(int index, int phase, int x, int y, int deltaX, int deltaY) {
        currentConfiguration.get(index).moveBy(deltaX, deltaY);
        emitRobotEvent(index, SimulationEventType.ROBOT_MOVING, phase, x, y, x + deltaX, y + deltaY);
        statistics.incrementStepCounter(index);
        statistics.incrementPhaseCounter(index, phase);
        List<SER> SERs = getSER();
        statistics.trackSERSize(SERs.get(0).getWidth(), SERs.get(0).getHeight());
        checkForCollisions();
    }
    
    private void emitRobotEvent(int index, SimulationEventType type, int phase, int startX, int startY) {
        emitRobotEvent(index, type, phase, startX, startY, startX, startY);
    }
    
    private void emitRobotEvent(int index, SimulationEventType type, int phase, int startX, int startY, int endX, int endY) {
        globalEventEmitter.emitEvent(new SimulationEvent(index, type, phase, startX, startY, endX, endY));
    }
    
    public SimulationStatistics getStatistics() {
        return statistics;
    }
    
    public boolean isComplete() {
        return completed;
    }

    private void endSimulation() {
        this.completed = true;
        this.statistics.setEndTime(Instant.now());
    }
}
