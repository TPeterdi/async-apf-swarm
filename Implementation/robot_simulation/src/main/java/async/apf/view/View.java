package async.apf.view;
import async.apf.controller.Controller;
import async.apf.interfaces.IController;
import async.apf.interfaces.IModel;
import async.apf.interfaces.IView;
import async.apf.model.Model;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;
import async.apf.view.elements.LabeledPositiveIntegerField;
import async.apf.view.elements.simulation.SimulationWindow;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


public class View extends Application implements IView {
    public ViewMethods viewMethods;
    private final EventEmitter globalEventEmitter = new EventEmitter();
    private SimulationWindow simulationWindow;

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
            this.simulationWindow = new SimulationWindow(globalEventEmitter, viewMethods.initialStates, viewMethods.targetStates);
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

    @Override
    public void handleEvent(SimulationEvent event) {
        if (simulationWindow != null) {
            simulationWindow.handleEvent(event);
        }
    }
}