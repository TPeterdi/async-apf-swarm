package async.apf.controller;

import java.util.List;

import async.apf.interfaces.IController;
import async.apf.interfaces.IEvent;
import async.apf.interfaces.IModel;
import async.apf.interfaces.IView;
import async.apf.model.Coordinate;
import async.apf.model.events.SimulationEvent;
import async.apf.view.events.ViewCoordinatesEvent;
import async.apf.view.events.ViewSimulationEvent;

public class Controller implements IController {
    private final IView view;
    private final IModel model;

    public Controller(IModel model, IView view) {
        this.model = model;
        this.view = view;
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
    public void setSimulationDelay(int delay) {
        this.model.setSimulationDelay(delay);
    }

    @Override
    public void beginSimulation() throws Exception {
        this.model.startSimulation();
    }

    @Override
    public void resumeSimulation() {
        this.model.resumeSimulation();
    }

    @Override
    public void pauseSimulation() {
        this.model.pauseSimulation();
    }

    @Override
    public void endSimulation() {
        this.model.endSimulation();
    }

    @Override
    public void restartSimulation() {
        this.model.restartSimulation();
    }

    @Override
    public void onEvent(IEvent event) {
        if (event instanceof SimulationEvent simulationEvent) {
            this.displayEvent(simulationEvent);
        }
        else if (event instanceof ViewCoordinatesEvent viewCoordinatesEvent) {
            switch (viewCoordinatesEvent.getEventType()) {
                case LOAD_INITIAL_CONFIG -> setStartingConfiguration(viewCoordinatesEvent.getCoordinates());
                case LOAD_TARGET_CONFIG -> setTargetPattern(viewCoordinatesEvent.getCoordinates());
                default ->
                        throw new IllegalStateException("Unexpected event type: " + viewCoordinatesEvent.getEventType());
            }
        }
        else if (event instanceof ViewSimulationEvent viewSimulationEvent) {
            switch (viewSimulationEvent.getEventType()) {
                case SIMULATION_START -> {
                    try {
                        beginSimulation();
                    } catch (Exception e) {
                        System.err.println(e.getMessage());
                    }
                }
                case SIMULATION_CONTINUE  -> resumeSimulation();
                case SIMULATION_PAUSE  -> pauseSimulation();
                case SIMULATION_END -> endSimulation();
                case SIMULATION_RESTART -> {
                    restartSimulation();
                }
                case SET_SIMULATION_DELAY -> setSimulationDelay(viewSimulationEvent.getDelay());
                default -> throw new IllegalStateException("Unexpected event type: " + viewSimulationEvent.getEventType());
            }
        }
    }
}