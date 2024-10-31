package async.apf.model;

import java.util.ArrayList;
import java.util.List;

import async.apf.interfaces.IEvent;
import async.apf.interfaces.IModel;
import async.apf.model.enums.SimulationEventType;
import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;
import async.apf.model.exceptions.InvalidInputException;

public class Model implements IModel {
    private List<Coordinate> loadedStartingConfiguration;
    private List<Coordinate> loadedTargetPattern;

    private final EventEmitter simulationEventEmitter;

    private Simulation currentSimulation;
    private int currentDelay;

    public Model(EventEmitter simulationEventEmitter) {
        this.simulationEventEmitter = simulationEventEmitter;
    }

    @Override
    public void storeStartingConfiguration(List<Coordinate> pattern) {
        this.loadedStartingConfiguration = new ArrayList<>();
        for (Coordinate coordinate : pattern) {
            this.loadedStartingConfiguration.add(coordinate.copy());
        }
    }

    @Override
    public void storeTargetPattern(List<Coordinate> pattern) {
        this.loadedTargetPattern = new ArrayList<>();
        for (Coordinate coordinate : pattern) {
            this.loadedTargetPattern.add(coordinate.copy());
        }
    }

    @Override
    public void setSimulationDelay(int delay) {
        this.currentDelay = delay;
        if (this.currentSimulation != null) {
            this.currentSimulation.setDelay(delay);
        }
    }

    @Override
    public void startSimulation() throws InvalidInputException {
        if (this.currentSimulation != null &&
            this.currentSimulation.hasBegun()) return;
        
        initializeSimulation();
        this.currentSimulation.begin();
    }

    private void initializeSimulation() throws InvalidInputException {
        List<Coordinate> startingConfigurationCopy = new ArrayList<>();
        for (Coordinate pos : this.loadedStartingConfiguration) {
            startingConfigurationCopy.add(new Coordinate(pos.getX(), pos.getY()));
        }
        List<Coordinate> targetPatternCopy = new ArrayList<>();
        for (Coordinate pos : this.loadedTargetPattern) {
            targetPatternCopy.add(new Coordinate(pos.getX(), pos.getY()));
        }

        if (this.currentSimulation != null) {
            this.simulationEventEmitter.removeEventListener(this.currentSimulation);
        }
        this.currentSimulation = new Simulation(this.simulationEventEmitter, startingConfigurationCopy, targetPatternCopy);
        this.currentSimulation.setDelay(this.currentDelay);
        this.simulationEventEmitter.addEventListener(this.currentSimulation);
    }

    @Override
    public void resumeSimulation() {
        if (this.currentSimulation.isPaused()) {
            this.currentSimulation.resume();
        }
    }

    @Override
    public void pauseSimulation() {
        if (this.currentSimulation.isRunning()) {
            this.currentSimulation.pause();
        }
    }

    @Override
    public void restartSimulation() {
        try {
            initializeSimulation();
        }
        catch (InvalidInputException e) {
            e.printStackTrace();
        }
        this.currentSimulation.begin();
    }

    @Override
    @SuppressWarnings("UseSpecificCatch")
    public void endSimulation() {
        try {
            this.initializeSimulation();
        }
        catch (Exception e) {
            System.err.println("Unexpected error occurred during reinitialization of simulation: " + e.getMessage());
        }
    }

    @Override
    public void onEvent(IEvent event) {
        if (event instanceof SimulationEvent simulationEvent && 
            simulationEvent.getEventType() == SimulationEventType.SIMULATION_END)
        {
        }
    }
    
}
