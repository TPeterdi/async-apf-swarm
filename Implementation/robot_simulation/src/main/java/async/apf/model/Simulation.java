package async.apf.model;

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

    // TODO: track statistics

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

        this.robots = new ArrayList<>();
        for (int i = 0; i < robotCount; i++) {
            this.robots.add(new Robot(globalEventEmitter));
        }

        this.simulationThread = new Thread(() -> {
            Random random = new Random();
            while (!this.completed) {
                if (this.isPaused) continue;

                int randomIndex = this.scheduler.pickNext();
                Robot pickedRobot = this.robots.get(randomIndex);
                try {
                    if (this.delay > 0) {
                        int desync = random.nextInt(20);
                        Thread.sleep(desync);
                    }
                    pickedRobot.activate(randomIndex, this.delay);
                }
                catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            }
            globalEventEmitter.emitEvent(new SimulationEvent(SimulationEventType.SIMULATION_END));
        });
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
        this.simulationThread.start();
    }

    public void resume() {
        this.isPaused = false;
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
    public synchronized void onEvent(IEvent event) {
        if (event instanceof RobotEvent robotEvent) {
            RobotEventType eventType = robotEvent.getEventType();
            int index = robotEvent.getId();
            Robot pickedRobot = robots.get(index);
            
            Coordinate currentCoordinate = currentConfiguration.get(index);
            int currentX = currentCoordinate.getX();
            int currentY = currentCoordinate.getY();

            int phase = ((RobotEvent) event).getPhase();

            switch (eventType) {
                case ACTIVE -> {}
                case STAY_PUT -> {}
                case LOOK -> {
                    globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_LOOKING, phase, currentX, currentY, currentX, currentY));
                    Coordinate robotLocation = currentConfiguration.get(index);
                    pickedRobot.supplyConfigurations(translateConfigurationToRobotsCoordinate(robotLocation), this.targetPattern);
                }
                case COMPUTE -> globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_COMPUTING, phase, currentX, currentY, currentX, currentY));
                case MOVE_NORTH -> {
                    currentCoordinate.moveBy(0, 1);
                    globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_MOVING, phase, currentX, currentY, currentX, currentY + 1));
                    checkForCollisions();
                }
                case MOVE_EAST -> {
                    currentCoordinate.moveBy(1, 0);
                    globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_MOVING, phase, currentX, currentY, currentX + 1, currentY));
                    checkForCollisions();
                }
                case MOVE_SOUTH -> {
                    currentCoordinate.moveBy(0, -1);
                    globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_MOVING, phase, currentX, currentY, currentX, currentY - 1));
                    checkForCollisions();
                }
                case MOVE_WEST -> {
                    currentCoordinate.moveBy(-1, 0);
                    globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_MOVING, phase, currentX, currentY, currentX - 1, currentY));
                    checkForCollisions();
                }
                case IDLE -> globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_IDLE, phase, currentX, currentY, currentX, currentY));
                case PATTERN_COMPLETE -> this.completed = true;
                default -> throw new IllegalArgumentException("Unexpected value: " + eventType);
            }
        }
    }
}
