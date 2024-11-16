package async.apf.view;

import java.util.List;
import java.util.Random;

import async.apf.model.Coordinate;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class BatchRunSettingsWindow {
    private final LabeledPositiveIntegerField batchSizeField;
    private final LabeledPositiveRangeField robotCountField;

    private final RadioButton initialAreaFileToggle;
    private final RadioButton initialAreaRangeToggle;
    private final FileInputField initialAreaFileField;
    private final LabeledPositiveRangeField initialAreaWidthField;
    private final LabeledPositiveRangeField initialAreaHeightField;

    private final RadioButton targetAreaFileToggle;
    private final RadioButton targetAreaRangeToggle;
    private final FileInputField targetAreaFileField;
    private final LabeledPositiveRangeField targetAreaWidthField;
    private final LabeledPositiveRangeField targetAreaHeightField;
    private final Button runBatchButton;

    public BatchRunSettingsWindow() {
        batchSizeField = new LabeledPositiveIntegerField("Batch Size", 100);
        robotCountField = new LabeledPositiveRangeField("Robot Count", 10, 20);

        initialAreaFileToggle = new RadioButton("Initial config: Fix");
        initialAreaFileField = new FileInputField("Initial config file");
        initialAreaRangeToggle = new RadioButton("Initial config: Range");
        initialAreaWidthField = new LabeledPositiveRangeField("Initial config width", 10, 20);
        initialAreaHeightField = new LabeledPositiveRangeField("Initial config height", 10, 20);
        
        targetAreaFileToggle = new RadioButton("Target pattern: Fix");
        targetAreaRangeToggle = new RadioButton("Target pattern: Range");
        targetAreaFileField = new FileInputField("Target pattern file");
        targetAreaWidthField = new LabeledPositiveRangeField("Target pattern width", 10, 20);
        targetAreaHeightField = new LabeledPositiveRangeField("Target pattern height", 10, 20);

        runBatchButton = new Button("Run Batch");
    }

    public void openBatchRunSettingsWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Batch Run Settings");

        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setAlignment(Pos.CENTER);

        // Add inputs to the grid
        gridPane.add(batchSizeField, 0, 0);
        gridPane.add(robotCountField, 0, 1);

        // Add initial area inputs
        addInitialAreaInputs(gridPane);

        // Add target area inputs
        addTargetAreaInputs(gridPane);

        // Add "Run Batch" button
        runBatchButton.setOnAction(e -> runBatch());
        gridPane.add(runBatchButton, 0, 10, 2, 1);

        Scene scene = new Scene(gridPane, 600, 500);
        newWindow.setScene(scene);
        newWindow.show();
    }

    private void addInitialAreaInputs(GridPane gridPane) {
        ToggleGroup initialAreaToggleGroup = new ToggleGroup();
        initialAreaFileToggle.setToggleGroup(initialAreaToggleGroup);
        initialAreaRangeToggle.setToggleGroup(initialAreaToggleGroup);
        initialAreaRangeToggle.setSelected(true);

        HBox initialAreaInput = new HBox(10,
            new VBox(10, initialAreaRangeToggle, initialAreaWidthField, initialAreaHeightField),
            new VBox(10, initialAreaFileToggle, initialAreaFileField)
        );
        gridPane.add(initialAreaInput, 0, 2, 2, 1);

        initialAreaFileField.setOnFileSelected(() -> updateRunBatchButtonState());
        initialAreaToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateRunBatchButtonState());
    }

    private void addTargetAreaInputs(GridPane gridPane) {
        ToggleGroup targetAreaToggleGroup = new ToggleGroup();
        targetAreaFileToggle.setToggleGroup(targetAreaToggleGroup);
        targetAreaRangeToggle.setToggleGroup(targetAreaToggleGroup);
        targetAreaRangeToggle.setSelected(true);

        HBox targetAreaInput = new HBox(10,
            new VBox(10, targetAreaRangeToggle, targetAreaWidthField, targetAreaHeightField),
            new VBox(10, targetAreaFileToggle, targetAreaFileField)
        );
        gridPane.add(targetAreaInput, 0, 6, 2, 1);

        targetAreaFileField.setOnFileSelected(() -> updateRunBatchButtonState());
        targetAreaToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> updateRunBatchButtonState());
    }

    private void updateRunBatchButtonState() {
        boolean isValid = validateSettings();
        runBatchButton.setDisable(!isValid);
    }

    private boolean validateSettings() {
        // Validate Initial Area settings
        if (!validateInitialArea()) return false;
        // Validate Target Area settings
        if (!validateTargetArea()) return false;
        // Validate robot count settings
        return validateRobotCount();
    }

    private boolean validateInitialArea() {
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
        return true;
    }

    private boolean validateTargetArea() {
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
        return true;
    }

    private boolean validateRobotCount() {
        return robotCountField.isValid();
    }

    private void runBatch() {
        Random rng = new Random();
        int batchSize = batchSizeField.getValue();

        for (int i = 0; i < batchSize; i++) {
            int robotCount = robotCountField.getRange()[1]; // Default to the upper range value

            List<Coordinate> initialConfig = getInitialConfig(robotCount, rng);
            List<Coordinate> targetPattern = getTargetPattern(robotCount, rng);

            startSimulation(robotCount, initialConfig, targetPattern);
        }
    }

    private List<Coordinate> getInitialConfig(int robotCount, Random rng) {
        if (initialAreaFileToggle.isSelected()) {
            return initialAreaFileField.getCoordinates();
        }
        else {
            int width = initialAreaWidthField.getRange()[0] + rng.nextInt(initialAreaWidthField.getRange()[1] - initialAreaWidthField.getRange()[0] + 1);
            int height = initialAreaHeightField.getRange()[0] + rng.nextInt(initialAreaHeightField.getRange()[1] - initialAreaHeightField.getRange()[0] + 1);
            return ViewMethods.generateCoordinates(robotCount, width, height);
        }
    }

    private List<Coordinate> getTargetPattern(int robotCount, Random rng) {
        if (targetAreaFileToggle.isSelected()) {
            return targetAreaFileField.getCoordinates();
        }
        else {
            int width = targetAreaWidthField.getRange()[0] + rng.nextInt(targetAreaWidthField.getRange()[1] - targetAreaWidthField.getRange()[0] + 1);
            int height = targetAreaHeightField.getRange()[0] + rng.nextInt(targetAreaHeightField.getRange()[1] - targetAreaHeightField.getRange()[0] + 1);
            return ViewMethods.generateCoordinates(robotCount, width, height);
        }
    }

    private void startSimulation(int robotCount, List<Coordinate> initialAreaCoordinates, List<Coordinate> targetAreaCoordinates) {
        System.out.println("Robot count: " + robotCount);
        System.out.println("Initial configuration coordinates: " + initialAreaCoordinates);
        System.out.println("Target pattern coordinates: " + targetAreaCoordinates);
        // Simulate the batch process here...
    }
}
