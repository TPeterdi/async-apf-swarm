package async.apf.view;

import async.apf.interfaces.IPositioned;
import async.apf.model.Coordinate;
import async.apf.model.RobotState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RobotViewState implements IPositioned {
    private int stepCount = 0;
    private RobotState state = RobotState.IDLE;
    private final Coordinate position;
    private boolean followed;
    private int lastPhase;

    public RobotViewState(Coordinate position) {
        super();
        this.position = position;
    }

    public int getStepCount() {
        return this.stepCount;
    }
    public void incrementStepCount() {
        this.stepCount += 1;
    }

    public RobotState getState() {
        return this.state;
    }

    public void setState(RobotState state) {
        this.state = state;
    }

    @Override
    public Coordinate getCoordinate() {
        return this.position;
    }

    public int getLastPhase() {
        return this.lastPhase;
    }

    public void setLastPhase(int lastPhase) {
        this.lastPhase = lastPhase;
    }

    private static final int REGULAR_SIZE = 12;
    private static final int MOVING_SIZE = 18;
    @Override
    public void drawOnCanvas(GraphicsContext gc, double screenX, double screenY, double zoom) {
        Color color = switch (this.state) {
            case IDLE       -> Color.DARKGRAY;
            case LOOK       -> Color.CHOCOLATE;
            case COMPUTE    -> Color.BROWN;
            case MOVE       -> Color.LIMEGREEN;
            default         -> Color.BLACK;
        };
        gc.setFill(color);
        int targetSize = this.state == RobotState.MOVE
            ? MOVING_SIZE
            : REGULAR_SIZE;

        gc.fillOval(screenX - targetSize * zoom / 2,
            screenY - targetSize * zoom / 2,
            targetSize * zoom,
            targetSize * zoom
        );
        
        // If followed is true, draw a black outline
        if (this.followed) {
            gc.setStroke(Color.BLACK); // Set stroke color to black
            gc.setLineWidth(2); // Set the line width for the outline
            gc.strokeOval(screenX - targetSize * zoom / 2,
                          screenY - targetSize * zoom / 2,
                          targetSize * zoom,
                          targetSize * zoom
            );
        }
    }
    public String numberToRomanNumeral(int number) {
        return switch (number) {
            case 0 -> "-";
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            default -> throw new AssertionError();
        };
    }

    @Override
    public void hoverEffect(GraphicsContext gc, double  width, double height, double screenX, double screenY, double zoom) {
        // Prepare the text to display
        String stepCountInfo = String.format("Step Count: %d", stepCount);
        String stateInfo = String.format("State: %s", state);
        String phaseInfo = "Last phase: " + numberToRomanNumeral(lastPhase);

        // Set text properties
        gc.setFont(Font.font(20));

        // Calculate the position to draw the text in the bottom right corner
        double padding = 5.0; // Padding between lines
        double textX = width - 10;  // 10 pixels from the right edge
        double textY = height - 10; // Start Y at the bottom edge

        // Measure text height to calculate total height for background
        double textHeight = gc.getFont().getSize();

        // Calculate background rectangle coordinates
        double backgroundWidth = 200; // Fixed width for background
        double backgroundHeight = 80;

        // Background rectangle position
        double backgroundX = textX - backgroundWidth;
        double backgroundY = textY - backgroundHeight;

        // Draw the white background rectangle
        gc.setFill(Color.WHITE);
        gc.fillRect(backgroundX, backgroundY, backgroundWidth, backgroundHeight);

        // Set text color
        gc.setFill(Color.BLACK);

        // Draw each line of text
        gc.fillText(stepCountInfo, textX - backgroundWidth + 5, textY - textHeight * 3 + padding);
        gc.fillText(stateInfo,     textX - backgroundWidth + 5, textY - textHeight * 2 + padding);
        gc.fillText(phaseInfo,     textX - backgroundWidth + 5, textY - textHeight * 1 + padding);
    }

    @Override
    public boolean isFollowed() {
        return followed;
    }

    @Override
    public void follow() {
        followed = true;
    }

    @Override
    public void unfollow() {
        followed = false;
    }
}
