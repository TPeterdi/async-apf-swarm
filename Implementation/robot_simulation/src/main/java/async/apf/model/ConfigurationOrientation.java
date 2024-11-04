package async.apf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import async.apf.model.enums.Cardinal;

public class ConfigurationOrientation {
    private List<Coordinate> coordinates;
    private List<Boolean> binaryRepresentation;
    private Cardinal orientation;
    private boolean xMirrored;
    private int width;
    private int height;

    public ConfigurationOrientation(List<Boolean> binaryRepresentation, Cardinal orientation, boolean xMirrored, int width, int height) {
        this.binaryRepresentation = binaryRepresentation;
        this.orientation = orientation;
        this.xMirrored = xMirrored;
        this.width = width;
        this.height = height;

        createCoordinates();
    }

    private void createCoordinates() {
        this.coordinates = new ArrayList<>();
        for (int idx = 0; idx < binaryRepresentation.size(); idx++) {
            if (Boolean.TRUE.equals(binaryRepresentation.get(idx))) {
                this.coordinates.add(OrientationHelper.indexToCoordinate(idx, width));
            }
        }
    }
    
    public List<Boolean> getBinaryRepresentation() {
        return binaryRepresentation;
    }

    public String getBinaryString() {
        StringBuilder binaryString = new StringBuilder();

        for (Boolean bool : binaryRepresentation) {
            binaryString.append(Boolean.TRUE.equals(bool) ? "1" : "0");
        }

        return binaryString.toString();
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
        
        if (obj == null)
            return false;

        ConfigurationOrientation other = (ConfigurationOrientation) obj;

        for (int idx = 0; idx < coordinates.size(); idx++) {
            if (!coordinates.get(idx).equals(other.coordinates.get(idx))) {
                return false;
            }
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

        Coordinate diff = coordinates.get(0).difference(other.coordinates.get(0));
        for (int idx = 1; idx < coordinates.size() - 1; idx++) {
            Coordinate currentDifference = coordinates.get(idx).difference(other.coordinates.get(idx));
            if (!diff.equals(currentDifference)) {
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

        Coordinate diff = coordinates.get(1).difference(other.coordinates.get(1));
        for (int idx = 2; idx < coordinates.size() - 1; idx++) {
            Coordinate currentDifference = coordinates.get(idx).difference(other.coordinates.get(idx));
            if (!diff.equals(currentDifference)) {
                return false;
            }
        }
        return true;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void setOrientation(Cardinal newOrientation) {
        this.orientation = newOrientation;
    }

    public void adjustOrientationByCardinal(Cardinal newCardinal) {
        this.orientation = valueToCardinal(cardinalValue(this.orientation) + cardinalValue(newCardinal));
    }

    private int cardinalValue(Cardinal cardinal) {
        return switch (cardinal) {
            case NORTH -> 0;
            case WEST  -> 1;
            case SOUTH -> 2;
            case EAST  -> 3;
            default    -> -1;
        };
    }
    private Cardinal valueToCardinal(int value) {
        return switch ((value + 4) % 4) {
            case 0  -> Cardinal.NORTH;
            case 1  -> Cardinal.WEST;
            case 2  -> Cardinal.SOUTH;
            case 3  -> Cardinal.EAST;
            default -> null;
        };
    }

    // Returns how many clockwise rotations (90°) are between 'from' and 'to'.
    public int cardinalDifference(Cardinal from, Cardinal to) {
        return (cardinalValue(to) - cardinalValue(from) + 4) % 4;
    }
}
