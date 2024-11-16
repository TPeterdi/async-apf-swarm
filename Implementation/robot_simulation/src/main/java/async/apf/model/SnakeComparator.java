package async.apf.model;

import java.util.Comparator;

public class SnakeComparator implements Comparator<Coordinate> {
    @Override
    public int compare(Coordinate c1, Coordinate c2) {
        if (c1.equals(c2)) return 0;
        if (c1.getY() != c2.getY()) {
            return c1.getY() - c2.getY();
        }
        else {
            if (c1.getY() % 2 == 0) {
                return c1.getX() - c2.getX();
            }
            else {
                return c2.getX() - c1.getX();
            }
        }
    }
}