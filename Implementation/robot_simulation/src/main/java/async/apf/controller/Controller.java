package async.apf.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import async.apf.model.Event;
import async.apf.model.Robot;
import async.apf.model.RobotEventType;
import async.apf.view.View;

public class Controller {
    private View view;

    public Controller(View view) {
        this.view = view;
    }

    // Method to start the actions for k robots and handle events
    public void manageRobots(List<Robot> robots) {
        
        // Emit a global SIMULATION_START event
        view.handleEvent(new Event(RobotEventType.SIMULATION_START));

        // For each robot, perform actions and handle the events asynchronously
        for (Robot robot : robots) {
            // robot.performActions().thenAccept(event -> {
            //     view.handleEvent(event);
            // });
        }

        // Emit a global SIMULATION_END event after some time (for demonstration)
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(5000); // Wait for the robots to finish
                view.handleEvent(new Event(RobotEventType.SIMULATION_END));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}