package async.apf.interfaces;

import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.exceptions.InvalidInputException;

public interface IModel extends IEventListener {
    void storeStartingConfiguration(List<Coordinate> pattern);
    void storeTargetPattern(List<Coordinate> targetPattern);
    void setSimulationDelay(int delay);
    void startSimulation() throws InvalidInputException;
    void stopSimulation();
}
