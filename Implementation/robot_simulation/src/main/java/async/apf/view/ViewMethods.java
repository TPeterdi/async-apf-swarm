package async.apf.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import async.apf.model.Coordinate;
import async.apf.model.events.EventEmitter;
import async.apf.view.elements.batch_run.BatchRunSettingsWindow;
import async.apf.view.elements.initial_confiugration.InitialConfigurationWindow;
import async.apf.view.elements.target_pattern.TargetPatternWindow;
import async.apf.view.enums.ViewEventType;
import async.apf.view.events.ViewCoordinatesEvent;
import javafx.scene.control.Button;

public class ViewMethods {
    public Button simulationStartButton;

    private final List<String[]> initialStatesTemp = new ArrayList<>();
    private final List<String[]> targetStatesTemp = new ArrayList<>();

    public final List<RobotViewState> initialStates = new ArrayList<>();
    public final List<RobotViewState> initialStatesOriginal = new ArrayList<>();
    public final List<Coordinate> targetStates = new ArrayList<>();

    private final EventEmitter simulationEventEmitter;

    private final Random rng = new Random();

    public ViewMethods(EventEmitter simulationEventEmitter) {
        this.simulationEventEmitter = simulationEventEmitter;
        simulationStartButton = new Button("Simulation start");
        simulationStartButton.setDisable(true);
    }

    public void openInitialWindow() {
        InitialConfigurationWindow initialStateWindow = new InitialConfigurationWindow(simulationEventEmitter, initialStatesTemp, initialStatesOriginal, initialStates);
        initialStateWindow.open(this::checkStates);
    }

    public void openTargetWindow() {
        TargetPatternWindow targetStateWindow = new TargetPatternWindow(simulationEventEmitter, targetStatesTemp, targetStates);
        targetStateWindow.open(this::checkStates);
    }

    private void copyCoordinates() {
        for (RobotViewState state : this.initialStatesOriginal) {
            this.initialStates.add(new RobotViewState(state.getCoordinate().copy()));
        }
    }

    private List<Coordinate> getCoordinatesFromRobotStates(List<RobotViewState> states) {
        List<Coordinate> result = new ArrayList<>();
        for (RobotViewState state : states) {
            result.add(state.getCoordinate());
        }
        return result;
    }

    public void generateRandomInputs(int robotCount, int initMaxW, int initMaxH, int targetMaxW, int targetMaxH) throws Exception {
        if (robotCount < 1) throw new Exception("Robot count must be at least 1!");
        if (initMaxW < 1)   throw new Exception("Initial max width must be at least 1!");
        if (initMaxH < 1)   throw new Exception("Initial max height must be at least 1!");
        if (targetMaxW < 1) throw new Exception("Target max width must be at least 1!");
        if (targetMaxH < 1) throw new Exception("Target max height must be at least 1!");
        if (initMaxW * initMaxH < robotCount) throw new Exception("Robots can't fit the initial area!");
        if (targetMaxW * targetMaxH < robotCount) throw new Exception("Robots can't fit the target area!");

        this.initialStates.clear();
        this.targetStates.clear();
        this.initialStatesOriginal.clear();

        List<Coordinate> randomInitialPattern = generateCoordinates(rng, robotCount, initMaxW, initMaxH);
        List<Coordinate> randomTargetPattern = generateCoordinates(rng, robotCount, targetMaxW, targetMaxH);

        for (int idx = 0; idx < randomInitialPattern.size(); idx++) {
            this.initialStatesOriginal.add(new RobotViewState(randomInitialPattern.get(idx)));
            this.targetStates.add(randomTargetPattern.get(idx));
        }
        copyCoordinates();
        simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, getCoordinatesFromRobotStates(initialStates)));
        simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
        simulationStartButton.setDisable(false);
    }

    public static List<Coordinate> generateCoordinates(Random rng, int count, int width, int height) {
        Set<Coordinate> coordinates = new HashSet<>();

        // Ensure we do not ask for more unique points than possible
        if (count > width * height) {
            throw new IllegalArgumentException("Cannot generate more unique coordinates than the area size.");
        }

        while (coordinates.size() < count) {
            int x = rng.nextInt(width);
            int y = rng.nextInt(height);

            Coordinate point = new Coordinate(x, y);
            coordinates.add(point);
        }

        // Convert Set to List and return
        return new ArrayList<>(coordinates);
    }

    public void openBatchRunSettingsWindow() {
        BatchRunSettingsWindow batchRunSettingsWindow = new BatchRunSettingsWindow();
        batchRunSettingsWindow.openBatchRunSettingsWindow();
    }

    private void checkStates() {
        if (!(initialStates.isEmpty()) && !(targetStates.isEmpty())) {
            simulationStartButton.setDisable(false);
        }
    }
}
