module async.apf.view {
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens async.apf.view to javafx.fxml;
    exports async.apf.view;
}