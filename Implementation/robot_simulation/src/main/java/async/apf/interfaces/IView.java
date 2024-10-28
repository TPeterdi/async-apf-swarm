package async.apf.interfaces;

import async.apf.model.events.EventEmitter;
import async.apf.model.events.SimulationEvent;

public interface IView {
    void handleEvent(SimulationEvent event);

    // TODO: define interface
}