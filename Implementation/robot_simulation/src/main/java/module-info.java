module async.apf.view {
    requires javafx.controls;
    requires javafx.fxml;


    opens async.apf.view to javafx.fxml;
    exports async.apf.view;
}