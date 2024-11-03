package async.apf.view;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import async.apf.model.Coordinate;
import async.apf.model.events.EventEmitter;
import async.apf.view.enums.ViewEventType;
import async.apf.view.events.ViewCoordinatesEvent;
import async.apf.view.events.ViewSimulationEvent;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ViewMethods {
    private SimulationCanvas simulationCanvas;
    private VBox controlsBox;
    private VBox simulationControlsVBox;

    public Button simulationStartButton;
    public boolean isSimulationStarted = false;
    public boolean isSimulationRunning = false;
    public boolean isSimulationFinished = false;
    public Stage newWindow;

    private final List<String[]> initialStatesTemp = new ArrayList<>();
    private final List<String[]> targetStatesTemp = new ArrayList<>();

    public List<RobotViewState> initialStates = new ArrayList<>();
    public final List<RobotViewState> initialStatesOriginal = new ArrayList<>();
    public final List<Coordinate> targetStates = new ArrayList<>();

    private EventEmitter simulationEventEmitter;

    public ViewMethods(EventEmitter simulationEventEmitter) {
        this.simulationEventEmitter = simulationEventEmitter;
    }

    public void openInitialWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Initial state");

        Label label = new Label("Set the robots initial coordinates: ");
        Label exampleLabel = new Label("For example 1,2;3,4 ");

        TextField textField = new TextField();

        Button importButton = new Button("Import from file (csv)");
        importButton.setOnAction(e -> {
            openCsvFile(newWindow, initialStatesTemp);
            stringToRobotState(initialStatesTemp, initialStates);
            newWindow.close();
            simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, getCoordinatesFromRobotStates(initialStates)));
            checkStates();
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {initialStatesTemp.clear(); textField.clear(); checkStates();});

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> newWindow.close());

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            initialStatesTemp.add(textField.getText().split(";"));
            stringToRobotState(initialStatesTemp, initialStatesOriginal);
            copyCoordinates();
            newWindow.close();
            simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, getCoordinatesFromRobotStates(initialStates)));
            checkStates();
        });

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    private List<Coordinate> getCoordinatesFromRobotStates(List<RobotViewState> states) {
        List<Coordinate> result = new ArrayList<>();
        for (RobotViewState state : states) {
            result.add(state.getCoordinate());
        }
        return result;
    }

    public void openTargetWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Target state");

        Label label = new Label("Set the robots target coordinates: ");
        Label exampleLabel = new Label("For example 1,2;3,4 ");
        TextField textField = new TextField();

        Button importButton = new Button("Import from file (csv)");
        importButton.setOnAction(e -> {
            openCsvFile(newWindow, targetStatesTemp);
            stringToCoordinate(targetStatesTemp, targetStates);
            newWindow.close();
            simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
            checkStates();
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            targetStatesTemp.clear(); textField.clear(); checkStates();});

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {newWindow.close();});

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            targetStatesTemp.add(textField.getText().split(";"));
            stringToCoordinate(targetStatesTemp, targetStates);
            newWindow.close();
            simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
            checkStates();
        });

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    public void openSimulationWindow() {
        newWindow = new Stage();
        newWindow.setTitle("Simulation");

        simulationCanvas = new SimulationCanvas<RobotViewState>(400, 400, initialStates);
        Canvas targetCanvas = new SimulationCanvas<Coordinate>(400, 400, targetStates);

        Label simulationLabel = new Label("Simulation");
        Label targetLabel = new Label("Target State");
        simulationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        targetLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        VBox simulationBox = new VBox(5, simulationLabel, simulationCanvas);
        simulationBox.setAlignment(Pos.CENTER);
        VBox targetBox = new VBox(5, targetLabel, targetCanvas);
        targetBox.setAlignment(Pos.CENTER);

        HBox canvasesHBox = new HBox(20, simulationBox, targetBox);
        canvasesHBox.setAlignment(Pos.CENTER);

        Slider slider = createSlider();

        VBox layout = new VBox(10); // VBox for vertical alignment
        layout.setAlignment(Pos.CENTER); // Center alignment for all elements

        controlsBox = getvBox(newWindow); // Get VBox reference here
        simulationControlsVBox = controlsBox;

        // Add components to the layout in the desired order
        layout.getChildren().addAll(simulationControlsVBox, canvasesHBox, slider);

        Scene scene = new Scene(layout, 900, 600);
        newWindow.setScene(scene);
        newWindow.show();
    }

    private Slider createSlider() {
        // Create the slider with min and max values
        int initialDelayValue = 250;
        Slider slider = new Slider(0, 500, initialDelayValue);
        this.simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SET_SIMULATION_DELAY, initialDelayValue));
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(100);
        slider.setMinorTickCount(10);
        slider.setBlockIncrement(50);
        slider.setSnapToTicks(true);
        // Listener for slider value changes
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int sliderValue = newValue.intValue();
            this.simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SET_SIMULATION_DELAY, sliderValue));
        });
        return slider;
    }

    private VBox getvBox(Stage window) {
        Button controllButton = getMainButton(window);

        Button closeButton = getCloseButton(window);
        return new VBox(10, controllButton, closeButton);
    }

    private Button getMainButton(Stage window) {
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
            if(isSimulationFinished) {
                initialStates.clear();
                copyCoordinates();
                isSimulationFinished = false;
                isSimulationRunning = true;
                isSimulationStarted = true;
                refreshCanvas();
                try {
                    simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_RESTART));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                refreshControlsVBox(window);

            }
            else if(isSimulationRunning){
                isSimulationRunning = false;
                isSimulationFinished = false;
                isSimulationStarted = true;
                try {
                    simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_PAUSE));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                refreshControlsVBox(window);

            }
            else if(isSimulationStarted){
                isSimulationRunning = true;
                isSimulationFinished = false;
                isSimulationStarted = true;
                try {
                    simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_CONTINUE));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                refreshControlsVBox(window);

            }
            else {
                isSimulationFinished = false;
                isSimulationStarted = true;
                isSimulationRunning = true;
                try {
                    simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_START));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                refreshControlsVBox(window);
            }
        });
        return controllButton;
    }

    private Button getCloseButton(Stage window) {
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
            initialStates.clear();
            targetStates.clear();
            initialStatesOriginal.clear();
            copyCoordinates();
            window.close();
            // Close logic here if needed
        });
        return closeButton;
    }

    private void checkStates() {
        if (!(initialStatesTemp.isEmpty()) && !(targetStatesTemp.isEmpty())) {
            simulationStartButton.setDisable(false);
        }
    }

    private void copyCoordinates() {
        for (RobotViewState state : this.initialStatesOriginal) {
            this.initialStates.add(new RobotViewState(state.getCoordinate().copy()));
        }
    }

    private void openCsvFile(Stage stage, List<String[]> array){
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a CSV file");

        FileChooser.ExtensionFilter csvFilter = new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(csvFilter);

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadCsvData(file, array);
        }
    }

    private void loadCsvData(File file, List<String[]> array) {
        array.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                array.add(values);
            }

        } catch (IOException e) {
            System.out.println("Something wrong happened during file read: " + e.getMessage());
        }
    }

    private void stringToCoordinate(List<String[]> from, List<Coordinate> to){
        for (String[] row : from) {
            for (String entry : row) {
                String[] parts = entry.split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                to.add(new Coordinate(x, y));
            }
        }
    }

    private void stringToRobotState(List<String[]> from, List<RobotViewState> to){
        for (String[] row : from) {
            for (String entry : row) {
                String[] parts = entry.split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                to.add(new RobotViewState(new Coordinate(x, y)));
            }
        }
    }

    public void refreshCanvas() {
        simulationCanvas.refresh();
    }

    public void refreshCanvas(int x, int y) {
        simulationCanvas.refreshAt(x, y);
    }

    public void refreshControlsVBox(Stage window) {
        Platform.runLater(() -> {
            controlsBox.getChildren().clear();
            controlsBox.getChildren().add(getMainButton(window));
            controlsBox.getChildren().add(getCloseButton(window));
        });
    }

    public void generateRandomInputs(int robotCount, int initMaxW, int initMaxH, int targetMaxW, int targetMaxH) throws Exception {
        if (robotCount < 1) throw new Exception("Robot count must be at least 1!");
        if (initMaxW < 1) throw new Exception("Initial max width must be at least 1!");
        if (initMaxH < 1) throw new Exception("Initial max height must be at least 1!");
        if (targetMaxW < 1) throw new Exception("Target max width must be at least 1!");
        if (targetMaxH < 1) throw new Exception("Target max height must be at least 1!");
        if (initMaxW * initMaxH < robotCount) throw new Exception("Robots can't fit the initial area!");
        if (targetMaxW * targetMaxH < robotCount) throw new Exception("Robots can't fit the target area!");

        this.initialStates.clear();
        this.targetStates.clear();

        List<Coordinate> randomInitialPattern = generateCoordinates(robotCount, initMaxW, initMaxH);
        List<Coordinate> randomTargetPattern = generateCoordinates(robotCount, targetMaxW, targetMaxH);

        for (int idx = 0; idx < randomInitialPattern.size(); idx++) {
            this.initialStates.add(new RobotViewState(randomInitialPattern.get(idx)));
            this.targetStates.add(randomTargetPattern.get(idx));
        }
        simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, getCoordinatesFromRobotStates(initialStates)));
        simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
        simulationStartButton.setDisable(false);
    }

    private List<Coordinate> generateCoordinates(int count, int width, int height) {
        Set<Coordinate> coordinates = new HashSet<>();
        Random random = new Random();

        // Ensure we do not ask for more unique points than possible
        if (count > width * height) {
            throw new IllegalArgumentException("Cannot generate more unique coordinates than the area size.");
        }

        while (coordinates.size() < count) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);

            Coordinate point = new Coordinate(x, y);
            coordinates.add(point);
        }

        // Convert Set to List and return
        return new ArrayList<>(coordinates);
    }
}
