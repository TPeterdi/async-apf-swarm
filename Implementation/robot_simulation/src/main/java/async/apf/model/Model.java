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
    private boolean isSimulationRunning = false;

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
        if (this.isSimulationRunning) return;

        this.isSimulationRunning = true;
        
        this.currentSimulation = new Simulation(this.simulationEventEmitter, this.loadedStartingConfiguration, this.loadedTargetPattern);
        this.currentSimulation.setDelay(this.currentDelay);
        this.simulationEventEmitter.addEventListener(this.currentSimulation);
        this.currentSimulation.start();
    }
    @Override
    public void stopSimulation() {
        if (this.currentSimulation == null) return;
        if (!this.isSimulationRunning) return;

        this.currentSimulation.stop();
    }

    @Override
    public void onEvent(IEvent event) {
        if (event instanceof SimulationEvent simulationEvent && 
            simulationEvent.getEventType() == SimulationEventType.SIMULATION_END)
        {
            this.isSimulationRunning = false;
        }
    }
    
}
