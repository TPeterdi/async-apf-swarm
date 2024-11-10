package async.apf.model;

public class SER {
    private final int minX;
    private final int maxX;
    private final int minY;
    private final int maxY;

    public SER(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public int getWidth() {
        return Math.min(maxX - minX + 1, maxY - minY + 1);
    }
    public int getHeight() {
        return Math.max(maxX - minX + 1, maxY - minY + 1);
    }
}
