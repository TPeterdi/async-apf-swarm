package async.apf.model;

import java.util.ArrayList;
import java.util.List;

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
    private boolean isPaused = false;
    private boolean completed = false;

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
            while (!this.completed) {
                if (this.isPaused) continue;

                int randomIndex = this.scheduler.pickNext();
                Robot pickedRobot = this.robots.get(randomIndex);
                try {
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

    private void moveRobotBy(int robotIndex, int dx, int dy) {
        // No validation in exchange for speed (we should only allow cardinal movements by 1)
        Coordinate currentCoordinate = currentConfiguration.get(robotIndex);
        int currentX = currentCoordinate.getX();
        int currentY = currentCoordinate.getY();
        globalEventEmitter.emitEvent(new SimulationEvent(robotIndex, SimulationEventType.ROBOT_MOVING, currentX, currentY, currentX + dx, currentY + dy));
        currentCoordinate.moveBy(dx, dy);
    }

    private List<Coordinate> translateConfigurationToRobotsCoordinate(Coordinate robotCoordinate) {
        List<Coordinate> translatedCoordinates = new ArrayList<>();
        for (Coordinate coordinate : this.currentConfiguration) {
            translatedCoordinates.add(coordinate.translate(robotCoordinate));
        }
        return translatedCoordinates;
    }

    @Override
    public synchronized void onEvent(IEvent event) {
        if (event instanceof RobotEvent robotEvent) {
            RobotEventType eventType = robotEvent.getEventType();
            int index = robotEvent.getId();
            Robot pickedRobot = robots.get(index);
            
            switch (eventType) {
                case ACTIVE -> {
                    return;
                }
                case STAY_PUT -> {
                    return;
                }
                case LOOK -> {
                    globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_LOOKING));
                    Coordinate robotLocation = currentConfiguration.get(index);
                    pickedRobot.supplyConfigurations(translateConfigurationToRobotsCoordinate(robotLocation), this.targetPattern);
                }
                case COMPUTE -> globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_COMPUTING));
                case MOVE_NORTH -> moveRobotBy(index, 0, 1);
                case MOVE_EAST  -> moveRobotBy(index, 1, 0);
                case MOVE_SOUTH -> moveRobotBy(index, 0, -1);
                case MOVE_WEST  -> moveRobotBy(index, -1, 0);
                case IDLE -> globalEventEmitter.emitEvent(new SimulationEvent(index, SimulationEventType.ROBOT_IDLE));
                case PATTERN_COMPLETE -> this.completed = true;
                default -> throw new IllegalArgumentException("Unexpected value: " + eventType);
            }
        }
    }
}
