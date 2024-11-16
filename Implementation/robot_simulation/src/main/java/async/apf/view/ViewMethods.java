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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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
            stringToRobotState(initialStatesTemp, initialStatesOriginal);
            copyCoordinates();
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
        SimulationCanvas targetCanvas = new SimulationCanvas<Coordinate>(400, 400, targetStates);

        Label simulationLabel = new Label("Simulation");
        Label targetLabel = new Label("Target State");
        simulationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        simulationLabel.setMaxHeight(24);
        targetLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        targetLabel.setMaxHeight(24);

        VBox simulationBox = new VBox(0, simulationLabel, simulationCanvas);
        simulationBox.setAlignment(Pos.CENTER);
        VBox targetBox = new VBox(0, targetLabel, targetCanvas);
        targetBox.setAlignment(Pos.CENTER);

        HBox canvasesHBox = new HBox(0, simulationBox, targetBox);
        // Add listener to resize canvases proportionally to HBox size changes
        canvasesHBox.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double canvasWidth = newWidth.doubleValue() / 2;  // Divide width equally
            simulationCanvas.resizeCanvas(canvasWidth, canvasesHBox.getHeight() - simulationLabel.getHeight());
            targetCanvas.resizeCanvas(canvasWidth, canvasesHBox.getHeight() - targetLabel.getHeight());
        });

        canvasesHBox.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            simulationCanvas.resizeCanvas(simulationCanvas.getWidth(), newHeight.doubleValue() - simulationLabel.getHeight() - simulationBox.getSpacing());
            targetCanvas.resizeCanvas(targetCanvas.getWidth(), newHeight.doubleValue() - targetLabel.getHeight() - targetBox.getSpacing());
        });
        VBox.setVgrow(canvasesHBox, Priority.ALWAYS);


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

    private VBox getvBox(Stage window) {
        Button controllButton = getMainButton(window);

        Button closeButton = getCloseButton(window);
        VBox box = new VBox(10, controllButton, closeButton);
        box.setPrefHeight(100);
        return box;
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
            simulationStartButton.setDisable(true);
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
            this.initialStatesOriginal.add(new RobotViewState(randomInitialPattern.get(idx)));
            this.targetStates.add(randomTargetPattern.get(idx));
        }
        copyCoordinates();
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

    public void openBatchRunSettingsWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Batch Run Settings");

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);

        // Batch size input
        LabeledPositiveIntegerField batchSizeField = new LabeledPositiveIntegerField("Batch Size", 100);
        gridPane.add(batchSizeField, 0, 0);

        // Robot count input (editable by default)
        LabeledPositiveRangeField robotCountField = new LabeledPositiveRangeField("Robot Count", 10, 20);
        gridPane.add(robotCountField, 0, 1);

        // Initial area toggle and inputs
        ToggleGroup initialAreaToggleGroup = new ToggleGroup();
        RadioButton initialAreaFileToggle = new RadioButton("Initial config: Fix");
        RadioButton initialAreaRangeToggle = new RadioButton("Initial config: Range");
        initialAreaFileToggle.setToggleGroup(initialAreaToggleGroup);
        initialAreaRangeToggle.setToggleGroup(initialAreaToggleGroup);
        initialAreaRangeToggle.setSelected(true); // Default to range input

        // Initial area range input
        LabeledPositiveRangeField initialAreaWidthField = new LabeledPositiveRangeField("Initial config width", 10, 20);
        LabeledPositiveRangeField initialAreaHeightField = new LabeledPositiveRangeField("Initial config height", 10, 20);

        // Initial area file input
        FileInputField initialAreaFileField = new FileInputField("Initial config file");
        initialAreaFileField.setDisable(true); // Disable file input initially

        // Initial area input (range and file on the same line)
        HBox initialAreaInput = new HBox(10, 
            new VBox(10, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField),
            new VBox(10, initialAreaFileToggle, initialAreaFileField)
        );
        gridPane.add(initialAreaInput, 0, 2, 2, 1); // Spanning 2 columns

        // Target area toggle and inputs
        ToggleGroup targetAreaToggleGroup = new ToggleGroup();
        RadioButton targetAreaFileToggle = new RadioButton("Target pattern: Fix");
        RadioButton targetAreaRangeToggle = new RadioButton("Target pattern: Range");
        targetAreaFileToggle.setToggleGroup(targetAreaToggleGroup);
        targetAreaRangeToggle.setToggleGroup(targetAreaToggleGroup);
        targetAreaRangeToggle.setSelected(true); // Default to range input

        // Target area range input
        LabeledPositiveRangeField targetAreaWidthField = new LabeledPositiveRangeField("Target pattern width", 10, 20);
        LabeledPositiveRangeField targetAreaHeightField = new LabeledPositiveRangeField("Target pattern height", 10, 20);

        // Target area file input
        FileInputField targetAreaFileField = new FileInputField("Target pattern file");
        targetAreaFileField.setDisable(true); // Disable file input initially

        // Target area input (range and file on the same line)
        HBox targetAreaInput = new HBox(10, 
            new VBox(10, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField),
            new VBox(10, targetAreaFileToggle, targetAreaFileField)
        );
        gridPane.add(targetAreaInput, 0, 6, 2, 1); // Spanning 2 columns

        // "Run Batch" button
        Button runBatchButton = new Button("Run Batch");
        runBatchButton.setOnAction(e -> {
            // Handle file inputs and disable robot count editing
            int robotCount = robotCountField.getRange()[1]; // Default value

            if (initialAreaFileToggle.isSelected()) {
                robotCount = initialAreaFileField.getCoordinateCount();
            } else if (targetAreaFileToggle.isSelected()) {
                robotCount = targetAreaFileField.getCoordinateCount();
            }

            //TODO rework this method!
            runBatch(
                batchSizeField.getValue(),
                initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                robotCountField
            );
        });
        gridPane.add(runBatchButton, 0, 10, 2, 1);

        // File selection logic for Initial Area
        initialAreaFileField.setOnFileSelected(() -> {
            if (initialAreaFileField.hasValidFile()) {
                int coordinateCount = initialAreaFileField.getCoordinateCount();
                robotCountField.setValue(coordinateCount, coordinateCount); // Set robot count to coordinate count
                robotCountField.setDisable(true); // Disable robot count fields
                robotCountField.setEditable(false); // Make robot count fields non-editable
            } else {
                robotCountField.clear(); // Clear robot count if no valid file
                robotCountField.setDisable(false); // Enable robot count fields for manual input
                robotCountField.setEditable(true); // Make robot count fields editable
            }
            updateRunBatchButtonState(
                initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                robotCountField, runBatchButton
            );
        });
        // File selection logic for Target Area
        targetAreaFileField.setOnFileSelected(() -> {
            if (targetAreaFileField.hasValidFile()) {
                int coordinateCount = targetAreaFileField.getCoordinateCount();
                robotCountField.setValue(coordinateCount, coordinateCount); // Set robot count to coordinate count
                robotCountField.setDisable(true); // Disable robot count fields
                robotCountField.setEditable(false); // Make robot count fields non-editable
            } else {
                robotCountField.clear(); // Clear robot count if no valid file
                robotCountField.setDisable(false); // Enable robot count fields for manual input
                robotCountField.setEditable(true); // Make robot count fields editable
            }
            updateRunBatchButtonState(
                initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                robotCountField, runBatchButton
            );
        });
        // Toggle logic to handle file selection enabling/disabling and robot count updating
        initialAreaToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == initialAreaFileToggle) {
                initialAreaWidthField.setDisable(true);
                initialAreaHeightField.setDisable(true);
                initialAreaFileField.setDisable(false);
                robotCountField.setDisable(true); // Disable robot count fields
                robotCountField.setEditable(false); // Make robot count fields non-editable

                // Clear or update robot count based on file
                if (initialAreaFileField.hasValidFile()) {
                    int coordinateCount = initialAreaFileField.getCoordinateCount();
                    robotCountField.setValue(coordinateCount, coordinateCount);
                    robotCountField.setDisable(true); // Disable robot count fields
                    robotCountField.setEditable(false); // Make robot count fields non-editable
                } else {
                    robotCountField.clear();
                }
                targetAreaFileToggle.setSelected(false);
                targetAreaRangeToggle.setSelected(true);
            } else {
                initialAreaWidthField.setDisable(false);
                initialAreaHeightField.setDisable(false);
                initialAreaFileField.setDisable(true);
                robotCountField.setDisable(false); // Re-enable robot count fields for manual input
                robotCountField.setEditable(true); // Make robot count fields editable
            }
            updateRunBatchButtonState(
                initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                robotCountField, runBatchButton
            );
        });
        
        targetAreaToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == targetAreaFileToggle) {
                targetAreaWidthField.setDisable(true);
                targetAreaHeightField.setDisable(true);
                targetAreaFileField.setDisable(false);
                robotCountField.setDisable(true); // Disable robot count fields
                robotCountField.setEditable(false); // Make robot count fields non-editable
        
                // Clear or update robot count based on file
                if (targetAreaFileField.hasValidFile()) {
                    int coordinateCount = targetAreaFileField.getCoordinateCount();
                    robotCountField.setValue(coordinateCount, coordinateCount);
                    robotCountField.setDisable(true); // Disable robot count fields
                    robotCountField.setEditable(false); // Make robot count fields non-editable
                } else {
                    robotCountField.clear();
                }
                initialAreaFileToggle.setSelected(false);
                initialAreaRangeToggle.setSelected(true);
            } else {
                targetAreaWidthField.setDisable(false);
                targetAreaHeightField.setDisable(false);
                targetAreaFileField.setDisable(true);
                robotCountField.setDisable(false); // Re-enable robot count fields for manual input
                robotCountField.setEditable(true); // Make robot count fields editable
            }
            updateRunBatchButtonState(
                initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                robotCountField, runBatchButton
            );
        });

        robotCountField.setOnValueChanged(() -> {
            updateRunBatchButtonState(
                    initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                    targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                    robotCountField, runBatchButton
            );
        });

        initialAreaWidthField.setOnValueChanged(() -> {
            updateRunBatchButtonState(
                    initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                    targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                    robotCountField, runBatchButton
            );
        });
        
        initialAreaHeightField.setOnValueChanged(() -> {
            updateRunBatchButtonState(
                    initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                    targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                    robotCountField, runBatchButton
            );
        });
        
        targetAreaWidthField.setOnValueChanged(() -> {
            updateRunBatchButtonState(
                    initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                    targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                    robotCountField, runBatchButton
            );
        });
        
        targetAreaHeightField.setOnValueChanged(() -> {
            updateRunBatchButtonState(
                    initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
                    targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
                    robotCountField, runBatchButton
            );
        });
        
        Scene scene = new Scene(gridPane, 600, 500);
        newWindow.setScene(scene);
        newWindow.show();
    }

    private boolean validateSettings(
        RadioButton initialAreaFileToggle, 
        RadioButton initialAreaRangeToggle, 
        LabeledPositiveRangeField initialAreaWidthField, 
        LabeledPositiveRangeField initialAreaHeightField, 
        FileInputField initialAreaFileField,
        RadioButton targetAreaFileToggle,
        RadioButton targetAreaRangeToggle,
        LabeledPositiveRangeField targetAreaWidthField,
        LabeledPositiveRangeField targetAreaHeightField,
        FileInputField targetAreaFileField,
        LabeledPositiveRangeField robotCountField
    ) {
        // Validate Initial Area settings
        if (initialAreaFileToggle.isSelected()) {
            if (!initialAreaFileField.hasValidFile() || initialAreaFileField.getCoordinateCount() == 0)
                return false;  // Invalid file or no coordinates
            if (robotCountField.getRange()[0] != initialAreaFileField.getCoordinateCount())
                return false;  // Robot count doesn't match coordinate count
            if (robotCountField.getRange()[1] != initialAreaFileField.getCoordinateCount())
                return false;  // Robot count doesn't match coordinate count
        }
        else if (initialAreaRangeToggle.isSelected()) {
            if (!initialAreaWidthField.isValid())
                return false; 
            if (!initialAreaHeightField.isValid())
                return false; 
        }

        // Validate Target Area settings
        if (targetAreaFileToggle.isSelected()) {
            if (!targetAreaFileField.hasValidFile() || targetAreaFileField.getCoordinateCount() == 0)
                return false;  // Invalid file or no coordinates
            if (robotCountField.getRange()[0] != targetAreaFileField.getCoordinateCount())
                return false;  // Robot count doesn't match coordinate count
            if (robotCountField.getRange()[1] != targetAreaFileField.getCoordinateCount())
                return false;  // Robot count doesn't match coordinate count
        }
        else if (targetAreaRangeToggle.isSelected()) {
            if (!targetAreaWidthField.isValid())
                return false; 
            if (!targetAreaHeightField.isValid())
                return false; 
        }
        
        // Validate robot count settings
        if (!robotCountField.isValid())
            return false;
        if (robotCountField.getRange()[1] > initialAreaWidthField.getRange()[0] * initialAreaHeightField.getRange()[0] ||
            robotCountField.getRange()[1] >  targetAreaWidthField.getRange()[0] *  targetAreaHeightField.getRange()[0])
            return false;

        return true;
    }

    // Update the "Run Batch" button based on validation
    private void updateRunBatchButtonState(
        RadioButton initialAreaFileToggle, 
        RadioButton initialAreaRangeToggle, 
        LabeledPositiveRangeField initialAreaWidthField, 
        LabeledPositiveRangeField initialAreaHeightField, 
        FileInputField initialAreaFileField,
        RadioButton targetAreaFileToggle,
        RadioButton targetAreaRangeToggle,
        LabeledPositiveRangeField targetAreaWidthField,
        LabeledPositiveRangeField targetAreaHeightField,
        FileInputField targetAreaFileField,
        LabeledPositiveRangeField robotCountField,
        Button runBatchButton
    ) {
        boolean isValid = validateSettings(
            initialAreaFileToggle, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField, initialAreaFileField,
            targetAreaFileToggle, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField, targetAreaFileField,
            robotCountField);

        runBatchButton.setDisable(!isValid);
    }

    // Example stub method for runBatch
    private void runBatch(
        int batchSize,
        RadioButton initialAreaFileToggle, 
        RadioButton initialAreaRangeToggle, 
        LabeledPositiveRangeField initialAreaWidthField, 
        LabeledPositiveRangeField initialAreaHeightField, 
        FileInputField initialAreaFileField,
        RadioButton targetAreaFileToggle,
        RadioButton targetAreaRangeToggle,
        LabeledPositiveRangeField targetAreaWidthField,
        LabeledPositiveRangeField targetAreaHeightField,
        FileInputField targetAreaFileField,
        LabeledPositiveRangeField robotCountField
    ) {
        Random rng = new Random();
        for (int i = 0; i < batchSize; i++) {
            int robotCountMin = robotCountField.getRange()[0];
            int robotCountMax = robotCountField.getRange()[1];
            int robotCount = robotCountMin + rng.nextInt(robotCountMax - robotCountMin + 1);

            List<Coordinate> initialConfig;
            if (initialAreaRangeToggle.isSelected()) {
                int initialConfigWidthMin, initialConfigWidthMax,
                    initialConfigHeightMin, initialConfigHeightMax;
                initialConfigWidthMin = initialAreaWidthField.getRange()[0];
                initialConfigWidthMax = initialAreaWidthField.getRange()[0];
                int width = initialConfigWidthMin + rng.nextInt(initialConfigWidthMax - initialConfigWidthMin + 1);
                
                initialConfigHeightMin = initialAreaHeightField.getRange()[0];
                initialConfigHeightMax = initialAreaHeightField.getRange()[0];
                int height = initialConfigHeightMin + rng.nextInt(initialConfigHeightMax - initialConfigHeightMin + 1);

                initialConfig = generateCoordinates(robotCount, width, height);
            }
            else {
                initialConfig = initialAreaFileField.getCoordinates();
            }

            List<Coordinate> targetPattern;
            if (targetAreaRangeToggle.isSelected()) {
                int targetPatternWidthMin, targetPatternWidthMax,
                    targetPatternHeightMin, targetPatternHeightMax;
                targetPatternWidthMin = initialAreaWidthField.getRange()[0];
                targetPatternWidthMax = initialAreaWidthField.getRange()[0];
                int width = targetPatternWidthMin + rng.nextInt(targetPatternWidthMax - targetPatternWidthMin + 1);

                targetPatternHeightMin = initialAreaHeightField.getRange()[0];
                targetPatternHeightMax = initialAreaHeightField.getRange()[0];
                int height = targetPatternHeightMin + rng.nextInt(targetPatternHeightMax - targetPatternHeightMin + 1);
                
                targetPattern = generateCoordinates(robotCount, width, height);
            }
            else {
                targetPattern = initialAreaFileField.getCoordinates();
            }
            startSimulation(robotCount, initialConfig, targetPattern);
        }
    }

    private void startSimulation(
        int robotCount,
        List<Coordinate> initialAreaCoordinates,
        List<Coordinate> targetAreaCoordinates
    ) {
        // Perform batch operations using the given data
        System.out.println("Robot count: " + robotCount);
        System.out.println("Initial configuration coordinates: " + initialAreaCoordinates);
        System.out.println("Target pattern coordinates: " + targetAreaCoordinates);
    
        // Additional batch processing logic would go here...
    }
}
