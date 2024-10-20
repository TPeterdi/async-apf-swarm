package async.apf.model;

import async.apf.model.enums.Cardinal;

public class Coordinate {
    private int x;
    private int y;

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void moveBy(int x, int y) 
    {
        this.x += x;
        this.y += y;
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}