module async.apf.robot_simulation {
    requires javafx.controls;
    requires javafx.fxml;


    opens async.apf.robot_simulation to javafx.fxml;
    exports async.apf.robot_simulation;
}