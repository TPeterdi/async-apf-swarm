package async.apf.view.elements.target_pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.events.EventEmitter;
import async.apf.view.enums.ViewEventType;
import async.apf.view.events.ViewCoordinatesEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TargetPatternWindow {

    private final EventEmitter simulationEventEmitter;
    private final List<String[]> targetStatesTemp;
    private final List<Coordinate> targetStates;

    public TargetPatternWindow(
        EventEmitter simulationEventEmitter, 
        List<String[]> targetStatesTemp, 
        List<Coordinate> targetStates
    ) {
        this.simulationEventEmitter = simulationEventEmitter;
        this.targetStatesTemp = targetStatesTemp;
        this.targetStates = targetStates;
    }

    public void open(Runnable onCloseCallback) {
        Stage newWindow = new Stage();
        newWindow.setTitle("Target state");

        Label label = new Label("Set the robots target coordinates: ");
        Label exampleLabel = new Label("For example 1,2;3,4 ");
        TextField textField = new TextField();

        Button importButton = new Button("Import from file (csv)");
        importButton.setOnAction(e -> {
            openCsvFile(newWindow);
            processTargetStates();
            newWindow.close();
            if (onCloseCallback != null) onCloseCallback.run(); // Notify on close
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            targetStatesTemp.clear();
            textField.clear();
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            newWindow.close();
            if (onCloseCallback != null) onCloseCallback.run(); // Notify on close
        });

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            targetStatesTemp.add(textField.getText().split(";"));
            processTargetStates();
            newWindow.close();
            if (onCloseCallback != null) onCloseCallback.run(); // Notify on close
        });

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    private void processTargetStates() {
        stringToCoordinate(targetStatesTemp, targetStates);
        simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
    }

    private void openCsvFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a CSV file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            loadCsvData(file);
        }
    }

    private void loadCsvData(File file) {
        targetStatesTemp.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                targetStatesTemp.add(line.split(";"));
            }
        }
        catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private void stringToCoordinate(List<String[]> from, List<Coordinate> to) {
        for (String[] row : from) {
            for (String entry : row) {
                String[] parts = entry.split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                to.add(new Coordinate(x, y));
            }
        }
    }
}
