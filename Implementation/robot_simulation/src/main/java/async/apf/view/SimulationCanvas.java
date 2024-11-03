package async.apf.view;

import java.util.List;

import async.apf.interfaces.IPositioned;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public final class SimulationCanvas<T extends IPositioned> extends Canvas {
    private static final double MIN_ZOOM = 0.3;
    private static final double MAX_ZOOM = 5.0;
    private static final double GRID_SPACING = 20.0;

    private double cameraX = 0;
    private double cameraY = 0;
    private double zoom = 1.0;
    private double dragStartX;
    private double dragStartY;
    
    private final GraphicsContext gc;
    private final List<T> items;
    // Track the currently selected item
    private T selectedItem = null;

    // Fields to track the last hovered grid tile
    private int lastHoveredTileX = Integer.MIN_VALUE;
    private int lastHoveredTileY = Integer.MIN_VALUE;

    public SimulationCanvas(double width, double height, List<T> items) {
        super(width, height);
        this.items = items;
        this.gc = this.getGraphicsContext2D();

        // Mouse events
        this.setOnMousePressed(this::onMousePressed);
        this.setOnMouseDragged(this::onMouseDragged);
        this.setOnScroll(this::onScroll);
        this.setOnMouseMoved(this::onMouseMoved);

        this.fitView();
    }

    private void onMousePressed(MouseEvent e) {
        if (e.getButton() == MouseButton.PRIMARY) {
            dragStartX = e.getX();
            dragStartY = e.getY();

            double worldX = (e.getX() - (getWidth() / 2)) / zoom + cameraX; // Convert screen coordinates to world coordinates
            double worldY = -(e.getY() - (getHeight() / 2)) / zoom + cameraY;
    
            int tileX = (int)Math.floor(worldX / GRID_SPACING + 0.5);
            int tileY = (int)Math.floor(worldY / GRID_SPACING + 0.5);
    
            T item = findItem(tileX, tileY);
            if (item != null) {
                // Toggle selection
                if (selectedItem == item) {
                    // Deselect the item
                    selectedItem.unfollow();
                    refreshAt(selectedItem.getCoordinate().getX(), selectedItem.getCoordinate().getY());
                    selectedItem = null;
                    refreshAt(tileX, tileY);
                }
                else {
                    // Select the item
                    if (selectedItem != null) {
                        selectedItem.unfollow();
                        refreshAt(selectedItem.getCoordinate().getX(), selectedItem.getCoordinate().getY());
                    }
                    selectedItem = item;
                    item.follow();
                    refreshAt(tileX, tileY);
                }
            }
            // Redraw or update the hover info for the selected item
            updateTileInfo(tileX, tileY, worldX, worldY);
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

    private void onMouseMoved(MouseEvent e) {
        // Convert screen coordinates to world coordinates
        double mouseX = e.getX();
        double mouseY = e.getY();
    
        double worldX = (mouseX - getWidth() / 2) / zoom + cameraX;
        double worldY = -(mouseY - getHeight() / 2) / zoom + cameraY;
    
        // Calculate grid tile coordinates
        int tileX = (int) Math.floor(worldX / GRID_SPACING + 0.5);
        int tileY = (int) Math.floor(worldY / GRID_SPACING + 0.5);
    
        if (tileX != lastHoveredTileX || tileY != lastHoveredTileY) {
            lastHoveredTileX = tileX;
            lastHoveredTileY = tileY;
        }
        if (selectedItem != null) {
            selectedItem.hoverEffect(gc, getWidth(), getHeight(), worldX, worldY, zoom);
        } else {
            updateTileInfo(tileX, tileY, worldX, worldY);
        }
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
        drawPoints(width, height);
    }
    
    public void drawGridCell(int x, int y) {
        // Convert grid coordinates to world space coordinates
        double cellCenterWorldX = x * GRID_SPACING;
        double cellCenterWorldY = y * GRID_SPACING;

        // Convert world coordinates to screen coordinates
        double screenX = getWidth() / 2 + (cellCenterWorldX - cameraX) * zoom;
        double screenY = getHeight() / 2 - (cellCenterWorldY - cameraY) * zoom;

        // Calculate bounds for the cell, taking zoom into account
        double halfCellSize = GRID_SPACING * zoom / 2;
        double clearX = screenX - halfCellSize;
        double clearY = screenY - halfCellSize;
        double clearSize = GRID_SPACING * zoom;

        // Clear only the area around the specified cell
        gc.clearRect(clearX, clearY, clearSize, clearSize);

        // Draw the grid lines around this cell
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(zoom);

        // Draw vertical and horizontal lines for the cell borders
        gc.strokeLine(screenX - halfCellSize, screenY - halfCellSize, screenX + halfCellSize, screenY - halfCellSize);
        gc.strokeLine(screenX - halfCellSize, screenY + halfCellSize, screenX + halfCellSize, screenY + halfCellSize);
        gc.strokeLine(screenX - halfCellSize, screenY - halfCellSize, screenX - halfCellSize, screenY + halfCellSize);
        gc.strokeLine(screenX + halfCellSize, screenY - halfCellSize, screenX + halfCellSize, screenY + halfCellSize);

    
        // Draw any item located in this cell, if applicable
        T item = findItem(x, y);
        if (item != null) {
            item.drawOnCanvas(gc, screenX, screenY, zoom);
            if (lastHoveredTileX == x && lastHoveredTileY == y)
                updateTileInfo(x, y, screenX, screenY);
        }
    }

    private void drawPoints(double width, double height) {
        if (items == null) return;

        for (IPositioned item : items) {
            double screenX = width / 2 + (item.getCoordinate().getX() * GRID_SPACING - cameraX) * zoom;
            double screenY = height / 2 - (item.getCoordinate().getY() * GRID_SPACING - cameraY) * zoom;
            item.drawOnCanvas(gc, screenX, screenY, zoom);
        }
    }
    
    private T findItem(int x, int y) {
        for (T item : items)
            if (item.getCoordinate().getX() == x &&
                item.getCoordinate().getY() == y)
                return item;
        return null;
    }

    public void refresh() {
        drawGrid();
    }

    public void refreshAt(int x, int y) {
        drawGridCell(x, y);
    }

    public void fitView() {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Iterate over the points to find the min and max x, y values
        for (IPositioned point : this.items) {
            int x = point.getCoordinate().getX();
            int y = point.getCoordinate().getY();
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
    
        double boundingBoxWidth = (maxX - minX + 4) * GRID_SPACING;
        double boundingBoxHeight = (maxY - minY + 7) * GRID_SPACING;
    
        double zoomX = width / boundingBoxWidth;
        double zoomY = height / boundingBoxHeight;
    
        // Set the zoom to the smaller scale, constrained within MIN_ZOOM and MAX_ZOOM
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, Math.min(zoomX, zoomY)));
    
        // Redraw the grid with the new camera and zoom settings
        drawGrid();
    }

    private void updateTileInfo(int tileX, int tileY, double worldX, double worldY) {
        T item = findItem(tileX, tileY);
        if (item == null) {
            drawGrid();
        }
        else {
            item.hoverEffect(gc, getWidth(), getHeight(), worldX, worldY, zoom);
        }
    }
    
    public void resizeCanvas(double newWidth, double newHeight) {
        // Set new dimensions for the canvas
        setWidth(newWidth);
        setHeight(newHeight);
    
        // Recalculate camera position and zoom if necessary (e.g., using fitView)
        fitView();
    }
}
