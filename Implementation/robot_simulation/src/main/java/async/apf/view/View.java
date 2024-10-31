package async.apf.view;
import async.apf.controller.Controller;
import async.apf.interfaces.IController;
import async.apf.interfaces.IModel;
import async.apf.interfaces.IView;
import async.apf.model.Coordinate;
import async.apf.model.Model;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class View extends Application implements IView {
    public ViewMethods viewMethods;
    EventEmitter globalEventEmitter = new EventEmitter();

    public View(){
        IModel model            = new Model(this.globalEventEmitter);
        IController controller  = new Controller(model, this);
        this.globalEventEmitter.addEventListener(controller);
    }

    @Override
    public void start(Stage primaryStage) {
        viewMethods = new ViewMethods(globalEventEmitter);

        Button setInitialStateButton = new Button("Set initial state");
        Button setTargetStateButton = new Button("Set target state");
        viewMethods.simulationStartButton = new Button("Simulation start");
        viewMethods.simulationStartButton.setDisable(true);

        setInitialStateButton.setOnAction(e -> viewMethods.openInitialWindow());
        setTargetStateButton.setOnAction(e -> viewMethods.openTargetWindow());
        viewMethods.simulationStartButton.setOnAction(e -> {
            System.out.println("Simulation beginning...");
            System.out.println("Initial state: " + viewMethods.initialStates);
            System.out.println("Target state: " + viewMethods.targetStates);
            viewMethods.openSimulationWindow();
        });

        // Layout
        VBox layout = new VBox(10, setInitialStateButton, setTargetStateButton, viewMethods.simulationStartButton);
        Scene scene = new Scene(layout, 800, 600);

        // Set main window
        primaryStage.setTitle("Robot Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
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
                //TODO QUEUE
                //Queue eventQueues = new PriorityQueue();
                System.out.println("View: Simulation has started!");
                break;
            case SIMULATION_END:
                System.out.println("View: Simulation has ended!");
                viewMethods.isSimulationFinished = true;
                viewMethods.isSimulationRunning = false;
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
                for (Coordinate coord : viewMethods.initialStates){
                    if (coord.getX() == event.getFromX() && coord.getY() == event.getFromY()) {
                        coord.setX(event.getToX());
                        coord.setY(event.getToY());
                        break;
                    }
                }
                System.out.println("Initial state: " + viewMethods.initialStates);
                viewMethods.drawScene(viewMethods.canvas.getGraphicsContext2D(), viewMethods.maxX, viewMethods.maxY);

                break;
            case ROBOT_IDLE:
                System.out.println("View: Robot " + event.getRobotId() + " is idle.");
                break;
            default:
                break;
        }
    }
}