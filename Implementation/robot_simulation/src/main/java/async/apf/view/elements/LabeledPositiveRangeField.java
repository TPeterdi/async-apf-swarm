package async.apf.view.elements;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class LabeledPositiveRangeField extends HBox {

    private Label label;
    private TextField minField;
    private TextField maxField;

    // Constructor with label text and optional initial values
    public LabeledPositiveRangeField(String labelText, Integer minValue, Integer maxValue) {
        label = new Label(labelText);
        minField = new TextField();
        maxField = new TextField();
        minField.setPromptText("Min");
        maxField.setPromptText("Max");
        minField.setMaxWidth(50);
        maxField.setMaxWidth(50);

        // Apply initial values if provided and greater than 0
        if (minValue != null && minValue > 0) {
            minField.setText(String.valueOf(minValue));
        }
        if (maxValue != null && maxValue > 0) {
            maxField.setText(String.valueOf(maxValue));
        }

        // Add listeners to ensure only positive integer input
        minField.textProperty().addListener((observable, oldValue, newValue) -> validateField(minField, oldValue, newValue));
        maxField.textProperty().addListener((observable, oldValue, newValue) -> validateField(maxField, oldValue, newValue));

        // Add label, minField, and maxField to the HBox
        this.getChildren().addAll(label, minField, new Label("-"), maxField);
        this.setSpacing(10); // Space between components
    }

    // Overloaded constructor without initial values
    public LabeledPositiveRangeField(String labelText) {
        this(labelText, null, null); // Calls the main constructor with null values
    }

    // Method to validate the field input as a positive integer
    private void validateField(TextField field, String oldValue, String newValue) {
        if (newValue.isEmpty()) {
            return; // Allow empty input temporarily for editing
        }

        try {
            int value = Integer.parseInt(newValue);
            if (value <= 0) {
                field.setText(oldValue); // Revert if not positive
            }
        } catch (NumberFormatException e) {
            field.setText(oldValue); // Revert if not an integer
        }
    }

    // Getter for the range as an integer array [min, max]
    public int[] getRange() {
        try {
            int min = Integer.parseInt(minField.getText());
            int max = Integer.parseInt(maxField.getText());
            return new int[]{min, max};
        } catch (NumberFormatException e) {
            return new int[]{-1, -1}; // Default range if invalid
        }
    }

    // Set range values programmatically
    public void setRange(int min, int max) {
        if (min > 0) {
            minField.setText(String.valueOf(min));
        }
        if (max > 0) {
            maxField.setText(String.valueOf(max));
        }
    }

    // Setter for the range
    public void setValue(int min, int max) {
        if (min > 0) {
            minField.setText(String.valueOf(min));
        }
        if (max > 0) {
            maxField.setText(String.valueOf(max));
        }
    }

    // Method to clear both fields
    public void clear() {
        minField.clear();
        maxField.clear();
    }

    // Disable interaction and editing of the TextField
    public void setEditable(boolean editable) {
        minField.setEditable(editable);
        maxField.setEditable(editable);
    }

    public void setOnValueChanged(Runnable listener) {
        minField.textProperty().addListener((observable, oldValue, newValue) -> listener.run());
        maxField.textProperty().addListener((observable, oldValue, newValue) -> listener.run());
    }

    public boolean isValid() {
        try {
            int min = Integer.parseInt(minField.getText());
            int max = Integer.parseInt(maxField.getText());

            return min <= max && min > 0 && max > 0;
        }
        catch (NumberFormatException ex) {
            return false;
        }
    }
}
