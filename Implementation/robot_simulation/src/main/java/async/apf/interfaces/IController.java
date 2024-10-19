package async.apf.interfaces;

import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.events.SimulationEvent;

public interface IController extends IEventListener {
    void startApp(String[] args);
    void setPattern(List<Coordinate> pattern);
    void setTargetPattern(List<Coordinate> targetPattern);
    void displayEvent(SimulationEvent event);
    void startSimulation() throws Exception;
}