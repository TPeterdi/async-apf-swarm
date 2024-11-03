package async.apf.view;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class LabeledPositiveIntegerField extends HBox {

    private Label label;
    private TextField textField;

    // Constructor with label text and optional initial value
    public LabeledPositiveIntegerField(String labelText, Integer initialValue) {
        label = new Label(labelText);
        textField = new TextField();
        textField.setPromptText("Enter a positive integer");
        textField.setMaxWidth(50);

        // Apply initial value if provided and greater than 0
        if (initialValue != null && initialValue > 0) {
            textField.setText(String.valueOf(initialValue));
        }

        // Add listener to allow only positive integer input
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                return; // Allow empty input temporarily for editing
            }

            try {
                int value = Integer.parseInt(newValue);
                if (value <= 0) {
                    textField.setText(oldValue); // Revert if not positive
                }
            } catch (NumberFormatException e) {
                textField.setText(oldValue); // Revert if not an integer
            }
        });

        // Add label and text field to the HBox
        this.getChildren().addAll(label, textField);
        this.setSpacing(10); // Space between label and text field
    }

    // Overloaded constructor without an initial value
    public LabeledPositiveIntegerField(String labelText) {
        this(labelText, null); // Calls the main constructor with a null initial value
    }

    // Getter for the TextField's text value as an integer
    public int getValue() {
        try {
            return Integer.parseInt(textField.getText());
        } catch (NumberFormatException e) {
            return 0; // Default to 0 if empty or invalid
        }
    }

    // Setter for the TextField's value
    public void setValue(int value) {
        if (value > 0) {
            textField.setText(String.valueOf(value));
        }
    }
}
