package async.apf.model;

import java.util.Objects;

import async.apf.interfaces.IPositioned;
import async.apf.model.enums.Cardinal;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Coordinate implements IPositioned {
    private int x;
    private int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Coordinate copy() {
        return new Coordinate(x, y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void moveBy(int dx, int dy) 
    {
        this.x += dx;
        this.y += dy;
    }

    public Coordinate translate(Coordinate newOrigin) 
    {
        return new Coordinate(this.x - newOrigin.x, this.y - newOrigin.y);
    }

    public void translateInPlace(Coordinate newOrigin) 
    {
        this.x -= newOrigin.x;
        this.y -= newOrigin.y;
    }

    public void rotateByCardinal(Cardinal cardinal) 
    {
        int tmp = this.x;
        switch (cardinal) {
            case WEST -> {
                this.x = -this.y;
                this.y = tmp;
            }
            case EAST -> {
                this.x = this.y;
                this.y = -tmp;
            }
            case SOUTH -> {
                this.x = -this.x;
                this.y = -this.y;
            }
            default -> {
            }
        }
    }

    public void counterRotateByCardinal(Cardinal cardinal) 
    {
        int tmp = this.x;
        switch (cardinal) {
            case WEST -> {
                this.x = this.y;
                this.y = -tmp;
            }
            case EAST -> {
                this.x = -this.y;
                this.y = tmp;
            }
            case SOUTH -> {
                this.x = -this.x;
                this.y = -this.y;
            }
            default -> {
            }
        }
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Coordinate difference(Coordinate to) {
        return new Coordinate(to.x - x, to.y - y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        
        if (obj == null || getClass() != obj.getClass())
            return false;
        
        Coordinate other = (Coordinate) obj;
        return x == other.x && y == other.y;
    }

    // Override hashCode to maintain consistency with equals
    @Override
    public int hashCode() {
        return Objects.hash(x + "," + y);
    }

    private static final double POINT_RADIUS = 5.0;
    @Override
    public void drawOnCanvas(GraphicsContext gc, double screenX, double screenY, double zoom) {
        gc.setFill(Color.DODGERBLUE);
        gc.fillOval(
            screenX - POINT_RADIUS * zoom / 2,
            screenY - POINT_RADIUS * zoom / 2,
            POINT_RADIUS * zoom,
            POINT_RADIUS * zoom
        );
    }

    @Override
    public Coordinate getCoordinate() {
        return this;
    }

    @Override
    public void hoverEffect(GraphicsContext gc, double width, double height, double screenX, double screenY, double zoom) {
    }

    private boolean followed = false;
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