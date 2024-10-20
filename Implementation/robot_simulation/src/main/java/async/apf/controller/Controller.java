package async.apf.controller;

import java.util.List;

import async.apf.interfaces.IController;
import async.apf.interfaces.IEvent;
import async.apf.interfaces.IModel;
import async.apf.interfaces.IView;
import async.apf.model.Coordinate;
import async.apf.model.events.SimulationEvent;
import async.apf.view.View;
import javafx.application.Application;

public class Controller implements IController {
    private final IView view;
    private final IModel model;

    public Controller(IModel model, IView view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void startApp(String[] args) {
        Application.launch(View.class, args);
    }

    @Override
    public void setStartingConfiguration(List<Coordinate> pattern) {
        this.model.storeStartingConfiguration(pattern);
    }

    @Override
    public void setTargetPattern(List<Coordinate> pattern) {
        this.model.storeTargetPattern(pattern);
    }

    @Override
    public void displayEvent(SimulationEvent event) {
        this.view.handleEvent(event);
    }

    @Override
    public void startSimulation() throws Exception {
        this.model.startSimulation();
    }

    @Override
    public boolean isSimulationRunning() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onEvent(IEvent event) {
        if (event instanceof SimulationEvent simulationEvent)
            this.displayEvent(simulationEvent);
    }
}