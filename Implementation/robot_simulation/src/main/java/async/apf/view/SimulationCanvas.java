package async.apf.view;

import java.util.List;

import async.apf.model.Coordinate;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public final class SimulationCanvas extends Canvas {
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 5.0;
    private static final double GRID_SPACING = 20.0;
    private static final double POINT_RADIUS = 5.0;

    private double cameraX = 0;
    private double cameraY = 0;
    private double zoom = 1.0;
    private double dragStartX;
    private double dragStartY;
    
    private final GraphicsContext gc;
    private List<Coordinate> coordinates;

    private Color color;

    public SimulationCanvas(double width, double height, List<Coordinate> coordinates, Color color) {
        super(width, height);
        this.coordinates = coordinates;
        this.color = color;
        this.gc = this.getGraphicsContext2D();

        // Mouse events
        this.setOnMousePressed(this::onMousePressed);
        this.setOnMouseDragged(this::onMouseDragged);
        this.setOnScroll(this::onScroll);

        this.fitView();
    }

    public SimulationCanvas(double width, double height, List<Coordinate> coordinates) {
        this(width, height, coordinates, Color.BLUE);
    }

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            dragStartX = e.getX();
            dragStartY = e.getY();
        }
    }

    private void onMouseDragged(MouseEvent event) {
        if (event.getButton() == MouseButton.PRIMARY) {
            double dx = (event.getX() - dragStartX) / zoom;
            double dy = (event.getY() - dragStartY) / zoom;
            cameraX -= dx;
            cameraY += dy;
            dragStartX = event.getX();
            dragStartY = event.getY();
            drawGrid();
        }
    }

    private void onScroll(ScrollEvent event) {
        double deltaZoom = event.getDeltaY() > 0 ? 1.1 : 0.9;
        double newZoom = zoom * deltaZoom;
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));

        // Adjust camera position to center the zoom on mouse position
        double mouseX = event.getX();
        double mouseY = event.getY();
        double offsetX = (mouseX - getWidth() / 2) / zoom + cameraX;
        double offsetY = -(mouseY - getHeight() / 2) / zoom + cameraY;

        cameraX = offsetX - (mouseX - getWidth() / 2) / newZoom;
        cameraY = offsetY + (mouseY - getHeight() / 2) / newZoom;

        zoom = newZoom;
        drawGrid();
    }

    private void drawGrid() {
        double width = getWidth();
        double height = getHeight();
        gc.clearRect(0, 0, width, height);

        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(zoom);

        // Scaled grid spacing
        double scaledGridSpacing = GRID_SPACING * zoom;

        // Calculate grid offsets
        double startX = (width / 2 - cameraX * zoom) % scaledGridSpacing - scaledGridSpacing / 2;
        double startY = (cameraY * zoom + height / 2) % scaledGridSpacing - scaledGridSpacing / 2;

        // Draw vertical lines
        for (double x = startX; x < width; x += scaledGridSpacing) {
            gc.strokeLine(x, 0, x, height);
        }

        // Draw horizontal lines
        for (double y = startY; y < height; y += scaledGridSpacing) {
            gc.strokeLine(0, y, width, y);
        }

        // Draw origin marker
        // -- only use for debugging --
        // double originScreenX = width / 2 - cameraX * zoom;
        // double originScreenY = height / 2 + cameraY * zoom;
        // gc.setStroke(Color.RED);
        // gc.strokeLine(originScreenX - 10, originScreenY, originScreenX + 10, originScreenY);
        // gc.strokeLine(originScreenX, originScreenY - 10, originScreenX, originScreenY + 10);

        // Draw points on the grid
        drawPoints(this.color, width, height);
    }

    private void drawPoints(Color color, double width, double height) {
        if (coordinates == null) return;

        gc.setFill(color);

        for (Coordinate point : coordinates) {
            double screenX = width / 2 + (point.getX() * GRID_SPACING - cameraX) * zoom;
            double screenY = height / 2 - (point.getY() * GRID_SPACING - cameraY) * zoom;

            gc.fillOval(
                screenX - POINT_RADIUS * zoom / 2,
                screenY - POINT_RADIUS * zoom / 2,
                POINT_RADIUS * zoom,
                POINT_RADIUS * zoom
            );
        }
    }

    public void setDisplayedPoints(List<Coordinate> points) {
        this.coordinates = points;
        drawGrid();  // Redraw the grid with the new points
    }

    public void refresh() {
        drawGrid();
    }

    public void fitView() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Iterate over the points to find the min and max x, y values
        for (Coordinate point : this.coordinates) {
            int x = point.getX();
            int y = point.getY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // Calculate the center of the bounding box
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
    
        // Set camera position to center the bounding box
        cameraX = centerX * GRID_SPACING;
        cameraY = centerY * GRID_SPACING;
    
        // Calculate the required zoom level to fit the bounding box
        double width = getWidth();
        double height = getHeight();
    
        double boundingBoxWidth = (maxX - minX + 3) * GRID_SPACING;
        double boundingBoxHeight = (maxY - minY + 6) * GRID_SPACING;
    
        double zoomX = width / boundingBoxWidth;
        double zoomY = height / boundingBoxHeight;
    
        // Set the zoom to the smaller scale, constrained within MIN_ZOOM and MAX_ZOOM
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.min(zoomX, zoomY)));
    
        // Redraw the grid with the new camera and zoom settings
        drawGrid();
    }
}
