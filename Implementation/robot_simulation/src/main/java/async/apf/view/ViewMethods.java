package async.apf.view;
import async.apf.model.Coordinate;
import async.apf.model.events.EventEmitter;
import async.apf.view.enums.viewEventType;
import async.apf.view.events.viewCoordinatesEvent;
import async.apf.view.events.viewSimulationEvent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ViewMethods {
    private double mouseX;
    private double mouseY;
    private double translateX = 0;
    private double translateY = 0;
    private double scaleFactor = 1.0;
    public Button simulationStartButton;
    public Boolean isSimulationRunning = false;
    public Boolean isSimulationFinished = false;
    private final List<String[]> initialStatesTemp = new ArrayList<>();
    private final List<String[]> targetStatesTemp = new ArrayList<>();
    public final List<Coordinate> initialStates = new ArrayList<>();
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
            stringToCoordinate(initialStatesTemp, initialStates);
            newWindow.close();
            try {
                simulationEventEmitter.emitEvent(new viewCoordinatesEvent(viewEventType.LOAD_INITIAL_CONFIG, initialStates));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            checkStates();
        });
        //Button randomButton = new Button("Random");

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {initialStatesTemp.clear(); textField.clear(); checkStates();});

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> newWindow.close());

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
                    initialStatesTemp.add(textField.getText().split(";"));
                    stringToCoordinate(initialStatesTemp, initialStates);
                    newWindow.close();
            try {
                simulationEventEmitter.emitEvent(new viewCoordinatesEvent(viewEventType.LOAD_INITIAL_CONFIG, initialStates));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            checkStates();
                }
        );

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
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
            try {
                simulationEventEmitter.emitEvent(new viewCoordinatesEvent(viewEventType.LOAD_TARGET_CONFIG, targetStates));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            checkStates();
        });

        //Button randomButton = new Button("Random");

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
            try {
                simulationEventEmitter.emitEvent(new viewCoordinatesEvent(viewEventType.LOAD_TARGET_CONFIG, targetStates));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            checkStates();
                }
        );

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    public void openSimulationWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Simulation");

        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        drawGrid(gc, 1000,600);

        canvas.setOnMousePressed(event -> {
            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        });

        canvas.setOnMouseDragged(event -> {
            double deltaX = event.getSceneX() - mouseX;
            double deltaY = event.getSceneY() - mouseY;

            translateX += deltaX;
            translateY += deltaY;

            canvas.setTranslateX(translateX);
            canvas.setTranslateY(translateY);

            mouseX = event.getSceneX();
            mouseY = event.getSceneY();
        });

        canvas.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            scaleFactor *= zoomFactor;

            Scale scale = new Scale(zoomFactor, zoomFactor, event.getX(), event.getY());
            canvas.getTransforms().add(scale);

            event.consume();
        });

        Pane canvasPane = new StackPane(canvas);

        VBox layout = getvBox();
        HBox hBox = new HBox(10, layout, canvasPane);

        hBox.setStyle("-fx-padding: 10;");
        Scene scene = new Scene(hBox, 1000,600);

        newWindow.setScene(scene);
        newWindow.setMaximized(true);
        newWindow.show();
    }

    private VBox getvBox() {
        Button controllButton = new Button("Start");
        controllButton.setOnAction(e -> {
            if(isSimulationRunning){
                isSimulationRunning = false;
                try {
                    simulationEventEmitter.emitEvent(new viewSimulationEvent(viewEventType.SIMULATION_STOP));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                controllButton.setText("Continue");
            }
            else{
                isSimulationRunning = true;
                try {
                    simulationEventEmitter.emitEvent(new viewSimulationEvent(viewEventType.SIMULATION_START));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                controllButton.setText("Stop");
            }
            System.out.println(isSimulationRunning);
        });

        Button resetButton = new Button("Reset");
        resetButton.setVisible(isSimulationFinished);
        return new VBox(10, controllButton, resetButton);
    }


    private void checkStates() {
        if (!(initialStatesTemp.isEmpty()) && !(targetStatesTemp.isEmpty())) {
            simulationStartButton.setDisable(false);
        }
    }

    private void drawGrid(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(0.5);

        // Horizontal lines
        for (int i = 0; i < width; i += 20) {
            gc.strokeLine(i, 0, i, height);
        }

        // Vertical lines
        for (int i = 0; i < height; i += 20) {
            gc.strokeLine(0, i, width, i);
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
            System.out.println("CSV data loaded:");
            array.forEach(row -> System.out.println(String.join(", ", row)));

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

        for (Coordinate coord : to) {
            System.out.println("X: " + coord.getX() + ", Y: " + coord.getY());
        }
    }
}
