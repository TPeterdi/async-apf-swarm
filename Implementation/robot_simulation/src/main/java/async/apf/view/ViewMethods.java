package async.apf.view;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.events.EventEmitter;
import async.apf.view.enums.ViewEventType;
import async.apf.view.events.ViewCoordinatesEvent;
import async.apf.view.events.ViewSimulationEvent;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ViewMethods {
    private double mouseX;
    private double mouseY;
    private double translateX = 0;
    private double translateY = 0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double scaleFactor = 1.0;
    private final int cellSize = 20;

    public Canvas canvas;
    public int maxX;
    public int maxY;
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
                simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, initialStates));
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
                simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, initialStates));
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
                simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
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
                simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_TARGET_CONFIG, targetStates));
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

        calculateMaxCoordinates();

        this.canvas = new Canvas(400, 400);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        offsetX = canvas.getWidth() / 2 - (maxX * cellSize) / 2.0;
        offsetY = canvas.getHeight() / 2 - (maxY * cellSize) / 2.0;

        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(e -> onMouseDragged(e, gc, maxX, maxY));
        canvas.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = event.getDeltaY() > 0 ? 1.1 : 0.9;
            scaleFactor *= zoomFactor;
            drawScene(gc, maxX, maxY);
            event.consume();
        });

        drawScene(gc, maxX, maxY);

        // Create the slider with min and max values
        int initialDelayValue = 500;
        Slider slider = new Slider(0, 2000, initialDelayValue);
        this.simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SET_SIMULATION_DELAY, initialDelayValue));
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(250);
        slider.setMinorTickCount(50);
        slider.setBlockIncrement(50);
        slider.setSnapToTicks(true);

        // Listener for slider value changes
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            int sliderValue = newValue.intValue();
            this.simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SET_SIMULATION_DELAY, sliderValue));
        });

        // Create the layout
        StackPane canvasPane = new StackPane(canvas);
        canvasPane.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10); // VBox for vertical alignment
        layout.setAlignment(Pos.CENTER); // Center alignment for all elements

        // Add components to the layout in the desired order
        layout.getChildren().addAll(getvBox(), canvasPane, slider);

        Scene scene = new Scene(layout, 800, 600);
        newWindow.setScene(scene);
        newWindow.show();
    }

    private void onMousePressed(MouseEvent e) {
        mouseX = e.getSceneX();
        mouseY = e.getSceneY();
    }

    private void onMouseDragged(MouseEvent event, GraphicsContext gc, int maxX, int maxY) {
        double deltaX = event.getSceneX() - mouseX;
        double deltaY = event.getSceneY() - mouseY;
        translateX += deltaX;
        translateY += deltaY;
        mouseX = event.getSceneX();
        mouseY = event.getSceneY();

        drawScene(gc, maxX, maxY);
    }



    private VBox getvBox() {
        Button controllButton = new Button("Start");
        controllButton.setOnAction(e -> {
            if(isSimulationRunning){
                isSimulationRunning = false;
                try {
                    simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_STOP));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                controllButton.setText("Continue");
            }
            else{
                isSimulationRunning = true;
                try {
                    simulationEventEmitter.emitEvent(new ViewSimulationEvent(ViewEventType.SIMULATION_START));
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




    public void drawScene(GraphicsContext gc, int maxX, int maxY) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        gc.save();
        gc.translate(translateX, translateY);
        gc.scale(scaleFactor, scaleFactor);

        drawGrid(gc, maxX, maxY);
        drawPoints(gc);

        gc.restore();
    }

    private void drawGrid(GraphicsContext gc, int maxX, int maxY) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(0.5);

        // Horizontal lines
        for (int i = 0; i <= maxX * cellSize; i += cellSize) {
            gc.strokeLine(i + offsetX, offsetY, i + offsetX, maxY * cellSize + offsetY);
        }

        // Vertical lines
        for (int i = 0; i < maxY * cellSize; i += cellSize) {
            gc.strokeLine(offsetX , i + offsetY, maxX * cellSize + offsetX, i + offsetY);
        }
        gc.strokeLine(offsetX, maxY * cellSize + offsetY, maxX * cellSize + offsetX, maxY * cellSize + offsetY);
    }

    private void drawPoints(GraphicsContext gc){
        gc.setFill(Color.RED);
        int pointRadius = 6;
        for (Coordinate cord : initialStates){
            double x = cord.getX() * cellSize + offsetX;
            double y = cord.getY() * cellSize + offsetY;

            gc.fillOval(x - pointRadius / 2.0, y - pointRadius / 2.0, pointRadius, pointRadius);
        }
    }

    private void calculateMaxCoordinates() {
        int initialMaxX = initialStates.stream().mapToInt(Coordinate::getX).max().orElse(0);
        int targetMaxX = targetStates.stream().mapToInt(Coordinate::getX).max().orElse(0);
        this.maxX = Math.max(initialMaxX, targetMaxX) + 1;

        int initialMaxY = initialStates.stream().mapToInt(Coordinate::getY).max().orElse(0);
        int targetMaxY = targetStates.stream().mapToInt(Coordinate::getY).max().orElse(0);
        this.maxY = Math.max(initialMaxY, targetMaxY) + 4;
    }

    private void checkStates() {
        if (!(initialStatesTemp.isEmpty()) && !(targetStatesTemp.isEmpty())) {
            simulationStartButton.setDisable(false);
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
}
