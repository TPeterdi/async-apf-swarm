package async.apf.model;

import java.util.List;

import async.apf.model.enums.Cardinal;

public class ConfigurationOrientation {
    private final List<Boolean> binaryRepresentation;
    private final Cardinal orientation;
    private final boolean xMirrored;
    private final int width;
    private final int height;

    public ConfigurationOrientation(List<Boolean> binaryRepresentation, Cardinal orientation, boolean xMirrored, int width, int height) {
        this.binaryRepresentation = binaryRepresentation;
        this.orientation = orientation;
        this.xMirrored = xMirrored;
        this.width = width;
        this.height = height;
    }
    
    public List<Boolean> getBinaryRepresentation() {
        return binaryRepresentation;
    }

    public Cardinal getOrientation() {
        return orientation;
    }

    public boolean isXMirrored() {
        return xMirrored;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
}
