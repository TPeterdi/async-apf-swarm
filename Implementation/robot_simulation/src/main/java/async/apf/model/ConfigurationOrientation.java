package async.apf.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import async.apf.model.enums.Cardinal;

public class ConfigurationOrientation {
    private final List<Coordinate> coordinates;
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

        this.coordinates = new ArrayList<>();
        for (int idx = 0; idx < binaryRepresentation.size(); idx++) {
            if (Boolean.TRUE.equals(binaryRepresentation.get(idx))) {
                coordinates.add(OrientationHelper.indexToCoordinate(idx, width));
            }
        }
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
    public Coordinate getHeadPosition() {
        return coordinates.getFirst();
    }

    public Coordinate getTailPosition() {
        return coordinates.getLast();
    }

    public String binaryString() {
        return binaryRepresentation.stream()
            .map(b -> Boolean.TRUE.equals(b) ? "1" : "0") 
            .collect(Collectors.joining(""));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        
        if (obj == null || getClass() != obj.getClass())
            return false;
        
        ConfigurationOrientation other = (ConfigurationOrientation) obj;
        if (other.binaryRepresentation.size() != binaryRepresentation.size())
            return false;

        for (int idx = 0; idx < binaryRepresentation.size(); idx++) {
            if (!(other.binaryRepresentation.get(idx) ^ binaryRepresentation.get(idx)))
                return false;
            
        }
        return true;
    }

    // Override hashCode to maintain consistency with equals
    @Override
    public int hashCode() {
        return Objects.hash(binaryString());
    }

    public boolean primeEquals(Object obj) {
        if (this == obj)
            return true;
        
        if (obj == null)
            return false;
        
        ConfigurationOrientation other = (ConfigurationOrientation) obj;

        for (int idx = 0; idx < coordinates.size() - 1; idx++) {
            if (!coordinates.get(idx).equals(other.coordinates.get(idx))) {
                return false;
            }
        }
        return true;
    }

    public boolean primePrimeEquals(Object obj) {
        if (this == obj)
            return true;
        
        if (obj == null)
            return false;
        
        ConfigurationOrientation other = (ConfigurationOrientation) obj;

        for (int idx = 1; idx < coordinates.size() - 1; idx++) {
            if (!coordinates.get(idx).equals(other.coordinates.get(idx))) {
                return false;
            }
        }
        return true;
    }

    public boolean isCoordinateMarked(int x, int y) {
        if (x < 0 || x >= width) return false;
        if (y < 0 || y >= height) return false;

        int row = y / width;
        int xValue = (row % 2 == 0) ? width - x - 1 : x;
        int linearIndex = row * width + xValue;
        return binaryRepresentation.get(linearIndex);
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }
}
