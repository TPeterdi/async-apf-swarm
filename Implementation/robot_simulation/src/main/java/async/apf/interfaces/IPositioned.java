package async.apf.interfaces;

import async.apf.model.Coordinate;
import javafx.scene.canvas.GraphicsContext;

public interface IPositioned {
    public Coordinate getCoordinate();

    public void follow();
    public void unfollow();
    public boolean isFollowed();

    public void drawOnCanvas(GraphicsContext gc, double screenX, double screenY, double zoom);
    public void hoverEffect(GraphicsContext gc, double width, double height, double screenX, double screenY, double zoom);
}
