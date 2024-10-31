package async.apf.interfaces;

import java.util.List;

import async.apf.model.Coordinate;
import async.apf.model.events.SimulationEvent;

public interface IController extends IEventListener {
    void setStartingConfiguration(List<Coordinate> pattern);
    void setTargetPattern(List<Coordinate> targetPattern);
    void displayEvent(SimulationEvent event);
    void setSimulationDelay(int delay);
    void beginSimulation() throws Exception;
    void pauseSimulation();
    void resumeSimulation();
    void endSimulation();
    void restartSimulation();
}
