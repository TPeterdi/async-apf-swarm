package async.apf;

import async.apf.controller.Controller;
import async.apf.interfaces.IController;
import async.apf.interfaces.IModel;
import async.apf.interfaces.IView;
import async.apf.model.Model;
import async.apf.model.events.EventEmitter;
import async.apf.view.View;

public class Main {
    public static void main(String[] args) {
        EventEmitter globalEventEmitter = new EventEmitter();

        IModel model            = new Model(globalEventEmitter);
        IView view              = new View();
        IController controller  = new Controller(model, view);
        
        globalEventEmitter.addEventListener(controller);

        controller.startApp(args);
    }
}
