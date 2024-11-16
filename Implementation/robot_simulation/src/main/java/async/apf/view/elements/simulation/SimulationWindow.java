package async.apf.view.elements.simulation;

import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.RobotState;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;
import async.apf.view.RobotViewState;
import async.apf.view.enums.ViewEventType;
import async.apf.view.events.ViewSimulationEvent;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SimulationWindow {

    private final Stage window;
    private final SimulationCanvas<RobotViewState> simulationCanvas;
    private final SimulationCanvas<Coordinate> targetCanvas;
    private final EventEmitter simulationEventEmitter;
    private final VBox simulationControlsVBox;

    private boolean isSimulationStarted = false;
    private boolean isSimulationRunning = false;
    private boolean isSimulationFinished = false;

    private final List<RobotViewState> currentConfiguration;
    private final List<Coordinate> targetPattern;

    public SimulationWindow(
        EventEmitter simulationEventEmitter,
        List<RobotViewState> initialStates,
        List<Coordinate> targetStates
    ) {
        this.simulationEventEmitter = simulationEventEmitter;
        this.currentConfiguration = initialStates;
        this.targetPattern = targetStates;

        window = new Stage();
        simulationCanvas = new SimulationCanvas<>(400, 400, initialStates);
        targetCanvas = new SimulationCanvas<>(400, 400, targetStates);
        simulationControlsVBox = createControlsBox();

        initializeWindow();
    }

    private void initializeWindow() {
        window.setTitle("Simulation");

        // Labels
        Label simulationLabel = createLabel("Simulation");
        Label targetLabel = createLabel("Target State");

        // Layout for simulation and target canvases
        VBox simulationBox = new VBox(0, simulationLabel, simulationCanvas);
        simulationBox.setAlignment(Pos.CENTER);
        VBox targetBox = new VBox(0, targetLabel, targetCanvas);
        targetBox.setAlignment(Pos.CENTER);

        HBox canvasesHBox = new HBox(0, simulationBox, targetBox);
        addCanvasResizeListeners(canvasesHBox, simulationLabel, targetLabel);

        VBox.setVgrow(canvasesHBox, Priority.ALWAYS);

        // Slider for controls
        Slider slider = createSlider();

        // Controls layout
        VBox layout = new VBox(10); // VBox for vertical alignment
        layout.setAlignment(Pos.CENTER); // Center alignment for all elements


        // Add components to the layout in the desired order
        layout.getChildren().addAll(simulationControlsVBox, canvasesHBox, slider);

        // Set up the scene
        Scene scene = new Scene(layout, 900, 600);
        window.setScene(scene);
        window.show();
    }

    private Label createLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        label.setMaxHeight(24);
        return label;
    }

    private void addCanvasResizeListeners(HBox canvasesHBox, Label simulationLabel, Label targetLabel) {
        canvasesHBox.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double canvasWidth = newWidth.doubleValue() / 2; // Divide width equally
            simulationCanvas.resizeCanvas(canvasWidth, canvasesHBox.getHeight() - simulationLabel.getHeight());
            targetCanvas.resizeCanvas(canvasWidth, canvasesHBox.getHeight() - targetLabel.getHeight());
        });

        canvasesHBox.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            simulationCanvas.resizeCanvas(simulationCanvas.getWidth(),
                    newHeight.doubleValue() - simulationLabel.getHeight() - simulationControlsVBox.getSpacing());
            targetCanvas.resizeCanvas(targetCanvas.getWidth(),
                    newHeight.doubleValue() - targetLabel.getHeight() - simulationControlsVBox.getSpacing());
        });
    }

    private Slider createSlider() {
        // Create the slider with min and max values
        int initialDelayValue = 200;
        Slider slider = new Slider(0, 400, initialDelayValue);
        this.simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SET_SIMULATION_DELAY, initialDelayValue));
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(100);
        slider.setMinorTickCount(25);
        slider.setBlockIncrement(50);
        slider.setSnapToTicks(true);
        // Listener for slider value changes
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int sliderValue = newValue.intValue();
            this.simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SET_SIMULATION_DELAY, sliderValue));
        });
        slider.setPrefHeight(50);
        return slider;
    }

    private VBox createControlsBox() {
        Button controllButton = getMainButton();

        Button closeButton = getCloseButton();
        VBox box = new VBox(10, controllButton, closeButton);
        box.setPrefHeight(100);
        return box;
    }

    private Button getMainButton() {
        Button controllButton = new Button();
        if(isSimulationFinished){
            controllButton.setText("Restart");
        } else if (isSimulationRunning) {
            controllButton.setText("Pause");
        } else if (isSimulationStarted) {
            controllButton.setText("Continue");
        } else {
            controllButton.setText("Start");
        }

        controllButton.setOnAction(e -> {
            if(isSimulationFinished)
                restartSimulation();

            else if(isSimulationRunning)
                pauseSimulation();

            else if(isSimulationStarted)
                continueSimulation();

            else
                startSimulation();
        });
        return controllButton;
    }

    private void startSimulation() throws RuntimeException {
        isSimulationFinished = false;
        isSimulationStarted = true;
        isSimulationRunning = true;
        try {
            simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_START));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        refreshControlsVBox();
    }

    private void pauseSimulation() throws RuntimeException {
        isSimulationRunning = false;
        isSimulationFinished = false;
        isSimulationStarted = true;
        try {
            simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_PAUSE));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        refreshControlsVBox();
    }

    private void continueSimulation() throws RuntimeException {
        isSimulationRunning = true;
        isSimulationFinished = false;
        isSimulationStarted = true;
        try {
            simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_CONTINUE));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        refreshControlsVBox();
    }

    private void restartSimulation() throws RuntimeException {
        currentConfiguration.clear();
        isSimulationFinished = false;
        isSimulationRunning = true;
        isSimulationStarted = true;
        refreshCanvas();
        try {
            simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_RESTART));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        refreshControlsVBox();
    }

    private Button getCloseButton() {
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            isSimulationRunning = false;
            isSimulationFinished = false;
            isSimulationStarted = false;
            try {
                simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_END));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            currentConfiguration.clear();
            targetPattern.clear();
            window.close();
        });
        return closeButton;
    }


    public void refreshControlsVBox() {
        Platform.runLater(() -> {
            simulationControlsVBox.getChildren().clear();
            simulationControlsVBox.getChildren().add(getMainButton());
            simulationControlsVBox.getChildren().add(getCloseButton());
        });
    }

    public void refreshCanvas() {
        simulationCanvas.refresh();
    }

    public void refreshCanvas(int x, int y) {
        simulationCanvas.refreshAt(x, y);
    }





    
    public void handleEvent(SimulationEvent event) {
        if (event.isGlobalEvent()) {
            handleGlobalEvent(event);
        } else {
            handleRobotEvent(event);
        }
    }

    // Handle global events like SIMULATION_START and SIMULATION_END
    private void handleGlobalEvent(SimulationEvent event) {
        switch (event.getEventType()) {
            case SIMULATION_START ->
                //TODO QUEUE
                System.out.println("View: Simulation has started!");
            case SIMULATION_END -> {
                System.out.println("View: Simulation has ended!");
                isSimulationFinished = true;
                isSimulationRunning = false;
                refreshControlsVBox();
            }
            default -> {
            }
        }
    }

    // Handle robot-specific events
    private void handleRobotEvent(SimulationEvent event) {
        int fromX = event.getFromX();
        int fromY = event.getFromY();
        RobotViewState robot = findRobotAt(fromX, fromY);
        if (robot == null)
            return;
        
        int toX = event.getToX();
        int toY = event.getToY();

        switch (event.getEventType()) {
            case ROBOT_IDLE ->      robot.setState(RobotState.IDLE);
            case ROBOT_LOOKING ->   robot.setState(RobotState.LOOK);
            case ROBOT_COMPUTING -> robot.setState(RobotState.COMPUTE);
            case ROBOT_MOVING -> {
                System.out.println("View: Robot " + event.getRobotId() +
                    "\tcalculated PHASE " + event.getPhase() +
                    " and moved from (" + event.getFromX() + "," + event.getFromY() + ")"+
                            " to (" + event.getToX()   + "," + event.getToY()   + ")");
                robot.setLastPhase(event.getPhase());
                robot.incrementStepCount();
                robot.setState(RobotState.MOVE);
                robot.getCoordinate().setX(event.getToX());
                robot.getCoordinate().setY(event.getToY());
            }
            default -> {}
        }
        refreshCanvas(fromX, fromY);
        if (fromX != toX || fromY != toY) {
            refreshCanvas(toX, toY);
        }
    }

    private RobotViewState findRobotAt(int x, int y) {
        for (RobotViewState coord : currentConfiguration)
            if (coord.getCoordinate().getX() == x &&
                coord.getCoordinate().getY() == y)
                return coord;

        return null;
    }
}
