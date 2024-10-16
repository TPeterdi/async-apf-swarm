package async.apf.view;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        // controller.doSomething();
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}