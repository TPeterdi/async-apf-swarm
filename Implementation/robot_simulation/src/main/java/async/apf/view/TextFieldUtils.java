package async.apf.view;

import javafx.scene.control.TextField;

public class TextFieldUtils {
    private TextFieldUtils() {}
    
    // Utility method to restrict TextField input to integers greater than 1
    public static void restrictToNumberGreaterThanOne(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                return; // Allow empty input for editing
            }

            try {
                int value = Integer.parseInt(newValue);
                if (value < 1) {
                    textField.setText(oldValue); // Revert to the previous valid value
                }
            } catch (NumberFormatException e) {
                textField.setText(oldValue); // Revert to the previous valid value
            }
        });
    }
}