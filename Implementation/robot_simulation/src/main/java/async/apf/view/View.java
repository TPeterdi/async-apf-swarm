package async.apf.view;
import async.apf.model.events.SimulationEvent;
import async.apf.interfaces.IView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;

public class View extends Application implements IView {
    private String initialState = null;
    private String targetState = null;
    private Button simulationStartButton;
    private Boolean isSimulationRunning = false;
    private Boolean isSimulationFinished = false;
    private double mouseX;
    private double mouseY;
    private double translateX = 0;
    private double translateY = 0;
    private double scaleFactor = 1.0;

    @Override
    public void start(Stage primaryStage) {

        // Főablak három gombbal
        Button setInitialStateButton = new Button("Set initial state");
        Button setTargetStateButton = new Button("Set target state");
        simulationStartButton = new Button("Simulation start");
        simulationStartButton.setDisable(false);

        // Gomb események kezelése
        setInitialStateButton.setOnAction(e -> openInitialWindow());
        setTargetStateButton.setOnAction(e -> openTargetWindow());
        simulationStartButton.setOnAction(e -> {
            // A szimuláció indításához szükséges logika
            System.out.println("Simulation beginning...");
            System.out.println("Initial state: " + initialState);
            System.out.println("Target state: " + targetState);
            isSimulationRunning = true;
            openSimulationWindow();
        });

        // Layout
        VBox layout = new VBox(10, setInitialStateButton, setTargetStateButton, simulationStartButton);
        Scene scene = new Scene(layout, 800, 600);

        // Főablak beállítása
        primaryStage.setTitle("Robot Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void openInitialWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Initial state");

        Label label = new Label("Set the value:");
        TextField textField = new TextField();

        Button importButton = new Button("Import from file (csv)");
        Button randomButton = new Button("Random");
        Button resetButton = new Button("Reset");
        Button closeButton = new Button("Close");
        Button confirmButton = new Button("Confirm");

        confirmButton.setOnAction(e -> {
                    initialState = textField.getText();
                    newWindow.close();
                    checkStates();
                }
            );

        VBox layout = new VBox(10, label, textField, importButton, randomButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    private void openTargetWindow() {
        Stage newWindow = new Stage();
        newWindow.setTitle("Target state");

        Label label = new Label("Set the value:");
        TextField textField = new TextField();

        Button importButton = new Button("Import from file (csv)");
        Button randomButton = new Button("Random");
        Button resetButton = new Button("Reset");
        Button closeButton = new Button("Close");
        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
                    targetState = textField.getText();
                    newWindow.close();
                    checkStates();
                }
        );

        VBox layout = new VBox(10, label, textField, importButton, randomButton, resetButton, closeButton, confirmButton);
        Scene scene = new Scene(layout, 400, 500);

        newWindow.setScene(scene);
        newWindow.show();
    }

    private void openSimulationWindow() {
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
        Button controllButton = new Button("Stop");
        controllButton.setOnAction(e -> {
            if(isSimulationRunning){
                isSimulationRunning = false;
                controllButton.setText("Continue");
            }
            else{
                isSimulationRunning = true;
                controllButton.setText("Stop");
            }
            System.out.println(isSimulationRunning);
        });

        Button resetButton = new Button("Reset");
        resetButton.setVisible(isSimulationFinished);
        VBox layout = new VBox(10, controllButton, resetButton);
        return layout;
    }

    private void checkStates() {
        if ((initialState != null && !initialState.trim().isEmpty()) && (targetState != null && !targetState.trim().isEmpty())) {
            simulationStartButton.setDisable(false);
        }
    }

    private void drawGrid(GraphicsContext gc, double width, double height) {
        gc.clearRect(0, 0, width, height);
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(0.5);

        // Vízszintes vonalak
        for (int i = 0; i < width; i += 20) {
            gc.strokeLine(i, 0, i, height);
        }

        // Függőleges vonalak
        for (int i = 0; i < height; i += 20) {
            gc.strokeLine(0, i, width, i);
        }
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
            case SIMULATION_START:
                System.out.println("View: Simulation has started!");
                break;
            case SIMULATION_END:
                System.out.println("View: Simulation has ended!");
                break;
            default:
                break;
        }
    }

    // Handle robot-specific events
    private void handleRobotEvent(SimulationEvent event) {
        switch (event.getEventType()) {
            case ROBOT_LOOKING:
                System.out.println("View: Robot " + event.getRobotId() + " is looking around.");
                break;
            case ROBOT_COMPUTING:
                System.out.println("View: Robot " + event.getRobotId() + " is computing data.");
                break;
            case ROBOT_MOVING:
                System.out.println("View: Robot " + event.getRobotId() + " is moving from (" +
                        event.getFromX() + "," + event.getFromY() + ") to (" +
                        event.getToX() + "," + event.getToY() + ").");
                break;
            case ROBOT_IDLE:
                System.out.println("View: Robot " + event.getRobotId() + " is idle.");
                break;
            default:
                break;
        }
    }
}