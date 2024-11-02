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
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class ViewMethods {
    private Canvas simulationCanvas;
    private VBox controlsBox;
    private VBox simulationControlsVBox;

    public Button simulationStartButton;
    public Boolean isSimulationStarted = false;
    public Boolean isSimulationRunning = false;
    public Boolean isSimulationFinished = false;
    public Stage newWindow;
    private final List<String[]> initialStatesTemp = new ArrayList<>();
    private final List<String[]> targetStatesTemp = new ArrayList<>();
    public List<Coordinate> initialStates = new ArrayList<>();
    public final List<Coordinate> initialStatesOriginal = new ArrayList<>();
    public final List<Coordinate> targetStates = new ArrayList<>();
    private EventEmitter simulationEventEmitter;

    private static final double GRID_SPACING = 50;    // Grid line spacing in world units
    private static final double MIN_ZOOM = 0.1;       // Minimum zoom level
    private static final double MAX_ZOOM = 5.0;       // Maximum zoom level
    private static final double POINT_RADIUS = 15.0;  // Radius of points

    private double cameraX = 0;                       // Camera's X position
    private double cameraY = 0;                       // Camera's Y position
    private double zoom = 1.0;                        // Current zoom level
    private double dragStartX, dragStartY;            // Initial mouse drag positions

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

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {initialStatesTemp.clear(); textField.clear(); checkStates();});

        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> newWindow.close());

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            initialStatesTemp.add(textField.getText().split(";"));
            stringToCoordinate(initialStatesTemp, initialStatesOriginal);
            copyCoordinates();
            newWindow.close();
            try {
                simulationEventEmitter.emitEvent(new ViewCoordinatesEvent(ViewEventType.LOAD_INITIAL_CONFIG, initialStates));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            checkStates();
        });

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
        });

        VBox layout = new VBox(10, label, exampleLabel, textField, importButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    public void openSimulationWindow() {
        newWindow = new Stage();
        newWindow.setTitle("Simulation");

        simulationCanvas = new Canvas(400, 400);
        Canvas targetCanvas = new Canvas(400, 400);
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        GraphicsContext gc2 = targetCanvas.getGraphicsContext2D();

        // Draw initial grid
        drawGrid(gc);
        drawGridTarget(gc2);

        simulationCanvas.setOnMousePressed(this::onMousePressed);
        simulationCanvas.setOnMouseDragged(e -> onMouseDragged(e, gc));
        simulationCanvas.setOnScroll(e -> onScroll(e, gc));

        targetCanvas.setOnMousePressed(this::onMousePressedTarget);
        targetCanvas.setOnMouseDragged(e -> onMouseDraggedTarget(e, gc2));
        targetCanvas.setOnScroll(e -> onScrollTarget(e, gc2));

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

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            dragStartX = e.getX();
            dragStartY = e.getY();
        }
    }

    private void onMouseDragged(MouseEvent event, GraphicsContext gc) {
        if (event.getButton() == MouseButton.PRIMARY) {
            double dx = (event.getX() - dragStartX) / zoom;
            double dy = (event.getY() - dragStartY) / zoom;
            cameraX -= dx;
            cameraY += dy;
            dragStartX = event.getX();
            dragStartY = event.getY();
            drawGrid(gc);
        }
    }

    private void onScroll(ScrollEvent event, GraphicsContext gc) {
        double deltaZoom = event.getDeltaY() > 0 ? 1.1 : 0.9;
        double newZoom = zoom * deltaZoom;
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

        // Calculate the new camera position so that zoom is centered, with inverted y-axis
        double mouseX = event.getX();
        double mouseY = event.getY();
        double offsetX = (mouseX - simulationCanvas.getWidth() / 2) / zoom + cameraX;
        double offsetY = -(mouseY - simulationCanvas.getHeight() / 2) / zoom + cameraY;  // Inverted y-axis

        cameraX = offsetX - (mouseX - simulationCanvas.getWidth() / 2) / newZoom;
        cameraY = offsetY + (mouseY - simulationCanvas.getHeight() / 2) / newZoom;       // Inverted y-axis

        zoom = newZoom;
        drawGrid(gc);
    }

    private void onMousePressedTarget(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            dragStartX = e.getX();
            dragStartY = e.getY();
        }
    }

    private void onMouseDraggedTarget(MouseEvent event, GraphicsContext gc) {
        if (event.getButton() == MouseButton.PRIMARY) {
            double dx = (event.getX() - dragStartX) / zoom;
            double dy = (event.getY() - dragStartY) / zoom;
            cameraX -= dx;
            cameraY += dy;
            dragStartX = event.getX();
            dragStartY = event.getY();
            drawGridTarget(gc);
        }
    }

    private void onScrollTarget(ScrollEvent event, GraphicsContext gc) {
        double deltaZoom = event.getDeltaY() > 0 ? 1.1 : 0.9;
        double newZoom = zoom * deltaZoom;
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

        // Calculate the new camera position so that zoom is centered, with inverted y-axis
        double mouseX = event.getX();
        double mouseY = event.getY();
        double offsetX = (mouseX - simulationCanvas.getWidth() / 2) / zoom + cameraX;
        double offsetY = -(mouseY - simulationCanvas.getHeight() / 2) / zoom + cameraY;  // Inverted y-axis

        cameraX = offsetX - (mouseX - simulationCanvas.getWidth() / 2) / newZoom;
        cameraY = offsetY + (mouseY - simulationCanvas.getHeight() / 2) / newZoom;       // Inverted y-axis

        zoom = newZoom;
        drawGridTarget(gc);
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

    private void drawGrid(GraphicsContext gc) {
        double width = simulationCanvas.getWidth();
        double height = simulationCanvas.getHeight();
        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(zoom);

        // Calculate the scaled grid spacing
        double scaledGridSpacing = GRID_SPACING * zoom;

        // Offset grid lines to center cells on integer coordinates, with zoom applied
        double startX = (width / 2 - cameraX * zoom) % scaledGridSpacing - scaledGridSpacing / 2;
        double startY = (cameraY * zoom + height / 2) % scaledGridSpacing - scaledGridSpacing / 2;

        // Draw vertical and horizontal lines to create grid cells, with scaled spacing
        for (double x = startX; x < width; x += scaledGridSpacing) {
            gc.strokeLine(x, 0, x, height);
        }
        for (double y = startY; y < height; y += scaledGridSpacing) {
            gc.strokeLine(0, y, width, y);
}

        // Draw origin marker at (0,0) in world space
        double originScreenX = width  / 2 - cameraX * zoom;
        double originScreenY = height / 2 + cameraY * zoom;
        gc.setStroke(Color.RED);
        gc.strokeLine(originScreenX - 10, originScreenY, originScreenX + 10, originScreenY);
        gc.strokeLine(originScreenX, originScreenY - 10, originScreenX, originScreenY + 10);

        // Draw points in global coordinates
        drawPoints(gc, width, height);
    }

    private void drawPoints(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.BLUE);

        for (Coordinate point : initialStates) {
            // Convert global coordinates to screen coordinates
            double screenX = width  / 2 + (point.getX() * GRID_SPACING - cameraX) * zoom;
            double screenY = height / 2 - (point.getY() * GRID_SPACING - cameraY) * zoom;

            // Draw the point as a small circle
            gc.fillOval(
                screenX - POINT_RADIUS * zoom / 2,
                screenY - POINT_RADIUS * zoom / 2,
                POINT_RADIUS * zoom,
                POINT_RADIUS * zoom);
        }
    }

    private void drawGridTarget(GraphicsContext gc) {
        double width = simulationCanvas.getWidth();
        double height = simulationCanvas.getHeight();
        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(zoom);

        // Calculate the scaled grid spacing
        double scaledGridSpacing = GRID_SPACING * zoom;

        // Offset grid lines to center cells on integer coordinates, with zoom applied
        double startX = (width / 2 - cameraX * zoom) % scaledGridSpacing - scaledGridSpacing / 2;
        double startY = (cameraY * zoom + height / 2) % scaledGridSpacing - scaledGridSpacing / 2;

        // Draw vertical and horizontal lines to create grid cells, with scaled spacing
        for (double x = startX; x < width; x += scaledGridSpacing) {
            gc.strokeLine(x, 0, x, height);
        }
        for (double y = startY; y < height; y += scaledGridSpacing) {
            gc.strokeLine(0, y, width, y);
        }

        // Draw origin marker at (0,0) in world space
        double originScreenX = width  / 2 - cameraX * zoom;
        double originScreenY = height / 2 + cameraY * zoom;
        gc.setStroke(Color.RED);
        gc.strokeLine(originScreenX - 10, originScreenY, originScreenX + 10, originScreenY);
        gc.strokeLine(originScreenX, originScreenY - 10, originScreenX, originScreenY + 10);

        // Draw points in global coordinates
        drawPointsTarget(gc, width, height);
    }

    private void drawPointsTarget(GraphicsContext gc, double width, double height) {
        gc.setFill(Color.RED);

        for (Coordinate point : targetStates) {
            // Convert global coordinates to screen coordinates
            double screenX = width  / 2 + (point.getX() * GRID_SPACING - cameraX) * zoom;
            double screenY = height / 2 - (point.getY() * GRID_SPACING - cameraY) * zoom;

            // Draw the point as a small circle
            gc.fillOval(
                    screenX - POINT_RADIUS * zoom / 2,
                    screenY - POINT_RADIUS * zoom / 2,
                    POINT_RADIUS * zoom,
                    POINT_RADIUS * zoom);
        }
    }

    private void checkStates() {
        if (!(initialStatesTemp.isEmpty()) && !(targetStatesTemp.isEmpty())) {
            simulationStartButton.setDisable(false);
        }
    }

    private void copyCoordinates() {
        for (Coordinate coord : this.initialStatesOriginal) {
            this.initialStates.add(coord.copy());
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

    public void refreshCanvas() {
        GraphicsContext gc = simulationCanvas.getGraphicsContext2D();
        drawGrid(gc);
    }

    public void refreshControlsVBox(Stage window) {
        Platform.runLater(() -> {
            controlsBox.getChildren().clear();
            controlsBox.getChildren().add(getMainButton(window));
            controlsBox.getChildren().add(getCloseButton(window));
        });
    }


}
