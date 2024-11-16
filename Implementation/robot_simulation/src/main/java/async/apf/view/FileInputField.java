package async.apf.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import async.apf.model.Coordinate;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;

public class FileInputField extends GridPane  {

    private Label label;
    private Label fileNameLabel;  // Label to display the file name
    private Button selectFileButton;
    private File selectedFile;
    private int coordinateCount;
    private int[] dimensions;
    private Runnable onFileSelected;

    private List<Coordinate> coordinates;

    public FileInputField(String labelText) {
        label = new Label(labelText);
        fileNameLabel = new Label("No file selected");
        selectFileButton = new Button("Select File");
        coordinateCount = 0;
        dimensions = new int[]{0, 0};

        // Set the layout grid
        this.setHgap(10);
        this.setVgap(10);

        // Position label and button in the first row (0,0)
        this.add(label, 0, 0);
        this.add(selectFileButton, 1, 0);

        // Position the file name label in the second row (0,1)
        this.add(fileNameLabel, 0, 1, 2, 1); // Span across two columns

        selectFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select File");
            selectedFile = fileChooser.showOpenDialog(null);

            if (selectedFile != null) {
                processFile();
            }
        });
    }

    private void processFile() {
        if (selectedFile != null) {
            try {
                String[] coordinateStrings = Files
                    .readAllLines(selectedFile.toPath())
                    .getFirst()
                    .split(";");

                this.coordinateCount = coordinateStrings.length;

                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
                this.coordinates = new ArrayList<>();

                for (String coordinateString : coordinateStrings) {
                    String[] parts = coordinateString.split(",");
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());

                    coordinates.add(new Coordinate(x, y));

                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }

                this.dimensions = new int[]{maxX - minX, maxY - minY};

                // Update the file name label
                fileNameLabel.setText("Loaded file: " + selectedFile.getName());

                if (onFileSelected != null) {
                    onFileSelected.run();
                }
            }
            catch (IOException | NumberFormatException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    public void setOnFileSelected(Runnable onFileSelected) {
        this.onFileSelected = onFileSelected;
    }
    
    public boolean hasValidFile() {
        return selectedFile != null && coordinateCount > 0;
    }

    public int getCoordinateCount() {
        return coordinateCount;
    }

    public int[] getDimensions() {
        return dimensions;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }
}
