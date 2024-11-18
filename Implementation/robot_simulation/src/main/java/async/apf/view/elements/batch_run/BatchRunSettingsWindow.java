package async.apf.view.elements.batch_run;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.swing.JFileChooser;

import async.apf.model.Coordinate;
import async.apf.model.Simulation;
import async.apf.model.SimulationStatistics;
import async.apf.model.events.EventEmitter;
import async.apf.view.ViewMethods;
import async.apf.view.elements.FileInputField;
import async.apf.view.elements.LabeledPositiveIntegerField;
import async.apf.view.elements.LabeledPositiveRangeField;
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

    private final Random rng = new Random();
    private final ExecutorService uiExecutor = Executors.newSingleThreadExecutor();

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

        addInitialAreaInputs(gridPane);
        addTargetAreaInputs(gridPane);

        runBatchButton.setOnAction(e -> uiExecutor.submit(this::runBatch));

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
        if (!validateInitialArea()) return false;
        if (!validateTargetArea()) return false;
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
        int batchSize = batchSizeField.getValue();
        List<Simulation> simulations = new ArrayList<>();
        List<SimulationStatistics> stats = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(batchSize);

        int maxConcurrentThreads = 4; // Limit concurrent simulations
        Semaphore semaphore = new Semaphore(maxConcurrentThreads);
        ExecutorService executor = Executors.newCachedThreadPool(); // Cached thread pool for dynamic thread management

        // Prepare simulation objects
        for (int i = 0; i < batchSize; i++) {
            int robotCount = robotCountField.getRange()[1]; // Default to the upper range value

            List<Coordinate> initialConfig = getInitialConfig(robotCount);
            List<Coordinate> targetPattern = getTargetPattern(robotCount);

            try {
                EventEmitter simulationEventEmitter = new EventEmitter();
                // Count finished simulations
                simulationEventEmitter.onEvent("SIMULATION_END", () -> {
                    latch.countDown();
                    semaphore.release();
                    System.out.println((batchSize - latch.getCount()) + " / " + batchSize + " simulations completed!");
                });
                Simulation newSimulation = new Simulation(simulationEventEmitter, initialConfig, targetPattern);
                simulations.add(newSimulation);
                simulationEventEmitter.addEventListener(newSimulation);
            }
            catch (Exception e) {
                
            }
        }

        // Run simulations
        try {
            for (Simulation simulation : simulations) {
                executor.submit(() -> {
                    try {
                        semaphore.acquire(); // Acquire a permit before starting
                        simulation.begin(); // Start the simulation
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // Wait for all simulations to complete
            latch.await();
            System.out.println("All objects have completed processing!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        // Get stats
        for (Simulation simulation : simulations)
            if (simulation.isComplete())
                stats.add(simulation.getStatistics());
        
        String summary = summarizeStats(stats);
        writeSummaryToFile(summary);
    }

    private List<Coordinate> getInitialConfig(int robotCount) {
        if (initialAreaFileToggle.isSelected()) {
            return initialAreaFileField.getCoordinates();
        }
        else {
            int width = initialAreaWidthField.getRange()[0] + rng.nextInt(initialAreaWidthField.getRange()[1] - initialAreaWidthField.getRange()[0] + 1);
            int height = initialAreaHeightField.getRange()[0] + rng.nextInt(initialAreaHeightField.getRange()[1] - initialAreaHeightField.getRange()[0] + 1);
            return ViewMethods.generateCoordinates(rng, robotCount, width, height);
        }
    }

    private List<Coordinate> getTargetPattern(int robotCount) {
        if (targetAreaFileToggle.isSelected()) {
            return targetAreaFileField.getCoordinates();
        }
        else {
            int width = targetAreaWidthField.getRange()[0] + rng.nextInt(targetAreaWidthField.getRange()[1] - targetAreaWidthField.getRange()[0] + 1);
            int height = targetAreaHeightField.getRange()[0] + rng.nextInt(targetAreaHeightField.getRange()[1] - targetAreaHeightField.getRange()[0] + 1);
            return ViewMethods.generateCoordinates(rng, robotCount, width, height);
        }
    }

    public static String summarizeStats(List<SimulationStatistics> stats) {
        if (stats == null || stats.isEmpty()) {
            return "The list is empty.";
        }

        // Example summary: Customize based on the type of data in the list
        StringBuilder summary = new StringBuilder();
        summary.append("Simulation logs:\n");
        summary.append("Total items: ").append(stats.size()).append("\n");

        // TODO: do proper mapping and aggregations

        return summary.toString();
    }

    public static void writeSummaryToFile(String summary) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Summary to File");

        // Show save dialog
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write(summary);
                System.out.println("Summary saved to file: " + fileToSave.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Failed to write summary to file: " + e.getMessage());
            }
        } else {
            System.out.println("Save operation cancelled by user.");
        }
    }
}