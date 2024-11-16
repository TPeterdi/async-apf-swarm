package async.apf.view.elements.initial_confiugration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.events.EventEmitter;
import async.apf.view.RobotViewState;
import async.apf.view.enums.ViewEventType;
import async.apf.view.events.ViewCoordinatesEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class InitialConfigurationWindow {

    private final EventEmitter simulationEventEmitter;
    private final List<String[]> initialStatesTemp;
    private final List<RobotViewState> initialStatesOriginal;
    private final List<RobotViewState> initialStates;

    public InitialConfigurationWindow(
        EventEmitter simulationEventEmitter, 
        List<String[]> initialStatesTemp, 
        List<RobotViewState> initialStatesOriginal, 
        List<RobotViewState> initialStates
    ) {
        this.simulationEventEmitter = simulationEventEmitter;
        this.initialStatesTemp = initialStatesTemp;
        this.initialStatesOriginal = initialStatesOriginal;
        this.initialStates = initialStates;
    }

    public void open(Runnable onCloseCallback) {
        Stage newWindow = new Stage();
        newWindow.setTitle("Initial state");

        Label label = new Label("Set the robots initial coordinates: ");
        Label exampleLabel = new Label("For example 1,2;3,4 ");

        TextField textField = new TextField();

        Button importButton = new Button("Import from file (csv)");
        importButton.setOnAction(e -> {
            openCsvFile(newWindow);
            processInitialStates();
            newWindow.close();
            if (onCloseCallback != null) onCloseCallback.run(); // Notify on close
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            initialStatesTemp.clear();
            textField.clear();
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> {
            newWindow.close();
            if (onCloseCallback != null) onCloseCallback.run(); // Notify on close
        });

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            initialStatesTemp.add(textField.getText().split(";"));
            processInitialStates();
            newWindow.close();
            if (onCloseCallback != null) onCloseCallback.run(); // Notify on close
        });

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    private void processInitialStates() {
        stringToRobotState(initialStatesTemp, initialStatesOriginal);
        copyCoordinates();
        simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, getCoordinatesFromRobotStates()));
    }

    private List<Coordinate> getCoordinatesFromRobotStates() {
        return initialStates.stream().map(RobotViewState::getCoordinate).toList();
    }

    private void copyCoordinates() {
        initialStates.clear();
        for (RobotViewState state : initialStatesOriginal) {
            initialStates.add(new RobotViewState(state.getCoordinate().copy()));
        }
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
        initialStatesTemp.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                initialStatesTemp.add(line.split(";"));
            }
        }
        catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    private void stringToRobotState(List<String[]> from, List<RobotViewState> to) {
        for (String[] row : from) {
            for (String entry : row) {
                String[] parts = entry.split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                to.add(new RobotViewState(new Coordinate(x, y)));
            }
        }
    }
}
