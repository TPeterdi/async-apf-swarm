package async.apf.interfaces;

import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.exceptions.InvalidInputException;

public interface IModel {
    void storeStartingConfiguration(List<Coordinate> pattern);
    void storeTargetPattern(List<Coordinate> targetPattern);
    void startSimulation() throws InvalidInputException;
}