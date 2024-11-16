package async.apf.view;
import async.apf.controller.Controller;
import async.apf.interfaces.IController;
import async.apf.interfaces.IModel;
import async.apf.interfaces.IView;
import async.apf.model.Model;
import async.apf.model.RobotState;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
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
        
        LabeledPositiveIntegerField robotCountField = new LabeledPositiveIntegerField("Robot count", 10);
        LabeledPositiveIntegerField randomInitialMaxWidthField = new LabeledPositiveIntegerField("Initial pattern max width", 6);
        LabeledPositiveIntegerField randomInitialMaxHeightField = new LabeledPositiveIntegerField("Initial pattern max height", 8);
        HBox randomInitialRow = new HBox(10, randomInitialMaxWidthField, randomInitialMaxHeightField);
        LabeledPositiveIntegerField randomTargetMaxWidthField = new LabeledPositiveIntegerField("Target pattern max width", 8);
        LabeledPositiveIntegerField randomTargetMaxHeightField = new LabeledPositiveIntegerField("Target pattern max height", 8);
        HBox randomTargetRow = new HBox(10, randomTargetMaxWidthField, randomTargetMaxHeightField);

        Button generateRandomPatternsButton = new Button("Randomize input");
        Button batchRunButton = new Button("Batch run");
        viewMethods.simulationStartButton = new Button("Simulation start");
        viewMethods.simulationStartButton.setDisable(true);

        setInitialStateButton.setOnAction(e -> viewMethods.openInitialWindow());
        setTargetStateButton.setOnAction(e -> viewMethods.openTargetWindow());
        generateRandomPatternsButton.setOnAction(e -> {
            try {
                viewMethods.generateRandomInputs(
                    robotCountField.getValue(),
                    randomInitialMaxWidthField.getValue(),
                    randomInitialMaxHeightField.getValue(),
                    randomTargetMaxWidthField.getValue(),
                    randomTargetMaxHeightField.getValue()
                );
            } catch (Exception e1) {
                // TODO Show error message
                e1.printStackTrace();
            }
        });
        batchRunButton.setOnAction(e -> viewMethods.openBatchRunSettingsWindow());
        viewMethods.simulationStartButton.setOnAction(e -> {
            System.out.println("Simulation beginning...");
            System.out.println("Initial state: " + viewMethods.initialStates);
            System.out.println("Target state: " + viewMethods.targetStates);
            viewMethods.openSimulationWindow();
        });

        // Layout
        VBox layout = new VBox(10,
            setInitialStateButton,
            setTargetStateButton,
            robotCountField,
            randomInitialRow,
            randomTargetRow,
            generateRandomPatternsButton,
            batchRunButton,
            viewMethods.simulationStartButton
            );
        Scene scene = new Scene(layout, 500, 300);

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
                viewMethods.refreshControlsVBox(viewMethods.newWindow);
                break;
            default:
                break;
        }
    }

    // Handle robot-specific events
    private void handleRobotEvent(SimulationEvent event) {
        int fx = event.getFromX();
        int fy = event.getFromY();
        RobotViewState robot = findRobotAt(fx, fy);
        if (robot == null)
            return;
        
        int tx = event.getToX();
        int ty = event.getToY();

        switch (event.getEventType()) {
            case ROBOT_IDLE ->      robot.setState(RobotState.IDLE);
            case ROBOT_LOOKING ->   robot.setState(RobotState.LOOK);
            case ROBOT_COMPUTING -> robot.setState(RobotState.COMPUTE);
            case ROBOT_MOVING -> {
                System.out.println("View: Robot " + event.getRobotId() +
                    "\tcalculated PHASE " + event.getPhase() +
                    " and moved from (" + event.getFromX() + "," + event.getFromY() + ")"+
                            " to (" + event.getToX()   + "," + event.getToY()   + ")");
                robot.setLastPhase(event.getPhase());
                robot.incrementStepCount();
                robot.setState(RobotState.MOVE);
                robot.getCoordinate().setX(event.getToX());
                robot.getCoordinate().setY(event.getToY());
            }
            default -> {}
        }
        viewMethods.refreshCanvas(fx, fy);
        if (fx != tx || fy != ty) {
            viewMethods.refreshCanvas(tx, ty);
        }
    }

    private RobotViewState findRobotAt(int x, int y) {
        for (RobotViewState coord : viewMethods.initialStates)
            if (coord.getCoordinate().getX() == x &&
                coord.getCoordinate().getY() == y)
                return coord;

        return null;
    }
}