package async.apf.model;

import java.util.List;

import async.apf.interfaces.IModel;
import async.apf.model.events.EventEmitter;
import async.apf.model.exceptions.InvalidInputException;

public class Model implements IModel {
    private List<Coordinate> loadedStartingConfiguration;
    private List<Coordinate> loadedTargetPattern;

    private final EventEmitter simulationEventEmitter;

    public Model(EventEmitter simulationEventEmitter) {
        this.simulationEventEmitter = simulationEventEmitter;
    }

    @Override
    public void storeStartingConfiguration(List<Coordinate> pattern) {
        this.loadedStartingConfiguration = pattern;
    }

    @Override
    public void storeTargetPattern(List<Coordinate> pattern) {
        this.loadedTargetPattern = pattern;
    }

    @Override
    public void startSimulation() throws InvalidInputException {
        new Simulation(this.simulationEventEmitter, this.loadedStartingConfiguration, this.loadedTargetPattern)
            .start();
    }
    
}
