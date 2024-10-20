package async.apf.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import async.apf.model.enums.RobotEventType;
import async.apf.model.enums.SimulationEventType;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;
import async.apf.model.exceptions.InvalidInputException;

public class Simulation {
    private final AsyncScheduler scheduler;
    private final EventEmitter globalEmitter;

    private final List<Coordinate> currentConfiguration;
    private final List<Coordinate> targetPattern;

    private final List<Robot> robots;

    private boolean running = false;

    // TODO: track statistics

    // Constructor to initialize the simulation with starting configuration, target pattern, and robots
    public Simulation(EventEmitter globalEmitter, List<Coordinate> startingConfiguration, List<Coordinate> targetPattern) throws InvalidInputException {
        this.globalEmitter = globalEmitter;
        this.currentConfiguration = startingConfiguration;
        this.targetPattern = targetPattern;

        int robotCount = startingConfiguration.size();

        if (robotCount != targetPattern.size()) {
            throw new InvalidInputException();
        }
        
        this.scheduler = new AsyncScheduler(robotCount);

        this.robots = IntStream.range(0, robotCount)
                               .mapToObj(i -> new Robot())
                               .collect(Collectors.toList());
    }

    public void start() {
        if (this.running) return;

        this.running = true;

        // Validation runs after each robot movement to match the current configuration with the target pattern.
        while (this.running) {
            int nextRobotIndex = this.scheduler.pickNext();
            Robot pickedRobot = this.robots.get(nextRobotIndex);
            pickedRobot.activate().thenAccept(robotEvent -> {
                RobotEventType eventType = robotEvent.getEventType();
                if (null != eventType)
                    switch (eventType) {
                    case ACTIVE -> {return;}
                    case STAY_PUT -> {return;}
                    case LOOK ->
                    {
                        globalEmitter.emitEvent(new SimulationEvent(nextRobotIndex, SimulationEventType.ROBOT_LOOKING));

                        Coordinate robotLocation = currentConfiguration.get(nextRobotIndex);
                        pickedRobot.supplyConfigurations(translateConfigurationToRobotsCoordinate(robotLocation), this.targetPattern);
                    }
                    case COMPUTE ->
                        globalEmitter.emitEvent(new SimulationEvent(nextRobotIndex, SimulationEventType.ROBOT_COMPUTING));
                    case MOVE_NORTH ->
                        moveRobotBy(nextRobotIndex, 0, 1);
                    case MOVE_EAST ->
                        moveRobotBy(nextRobotIndex, 1, 0);
                    case MOVE_SOUTH ->
                        moveRobotBy(nextRobotIndex, 0, -1);
                    case MOVE_WEST ->
                        moveRobotBy(nextRobotIndex, -1, 0);
                    case IDLE ->
                        globalEmitter.emitEvent(new SimulationEvent(nextRobotIndex, SimulationEventType.ROBOT_IDLE));
                    default ->
                        throw new IllegalArgumentException("Unexpected value: " + eventType);
                    }
            });
        }
    }

    private void moveRobotBy(int robotIndex, int dx, int dy) {
        // No validation in exchange for speed (should only allow cardinal movements by 1)
        Coordinate currentCoordinate = currentConfiguration.get(robotIndex);
        int currentX = currentCoordinate.getX();
        int currentY = currentCoordinate.getY();
        globalEmitter.emitEvent(new SimulationEvent(robotIndex, SimulationEventType.ROBOT_MOVING, currentX, currentY, currentX + dx, currentY + dy));
        currentCoordinate.moveBy(dx, dy);

        // We only update the running state after each move (since that's the only event that can achieve our goal).
        this.running = !patternCompleted();
    }

    private boolean patternCompleted() {
        // Count occurrences of each coordinate in the first list
        Map<Coordinate, Integer> countMap = new HashMap<>();
        for (Coordinate coord : targetPattern) {
            countMap.put(coord, countMap.getOrDefault(coord, 0) + 1);
        }

        // Decrease counts based on the second list
        for (Coordinate coord : currentConfiguration) {
            int count = countMap.getOrDefault(coord, 0);
            if (count == 0) {
                // If the coordinate was not found or already zero, lists are different
                return false;
            }
            countMap.put(coord, count - 1);
        }

        // If all counts are zero, the lists contain the same coordinates
        return true;
    }

    private List<Coordinate> translateConfigurationToRobotsCoordinate(Coordinate robotCoordinate) {
        List<Coordinate> translatedCoordinates = new ArrayList<>();
        for (Coordinate coordinate : this.currentConfiguration) {
            translatedCoordinates.add(coordinate.translate(robotCoordinate));
        }
        return translatedCoordinates;
    }

    public boolean isRunning() {
        return running;
    }
}
