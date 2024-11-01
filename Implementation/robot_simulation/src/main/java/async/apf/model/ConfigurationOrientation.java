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

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    public void changeOrientation(Cardinal newCardinal, boolean mirroredState) {
        changeMirroredState(mirroredState);
        rotateToCardinal(newCardinal);
        createCoordinates();
    }

    private void changeMirroredState(boolean target) {
        if (target != xMirrored) mirror();
    }

    private void mirror() {
        this.xMirrored = !this.xMirrored;

        List<Boolean> flippedBinaryRepresentation = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int tx = y % 2 == 0
                    ? width - x - 1
                    : x;
                flippedBinaryRepresentation.add(binaryRepresentation.get(y * width + tx));
            }
        }
        binaryRepresentation = flippedBinaryRepresentation;
    }

    private void rotateToCardinal(Cardinal newCardinal) {
        int rotations = cardinalDifference(this.orientation, newCardinal);
        if (rotations == 0) return;

        List<Boolean> rotatedBinaryRepresentation = new ArrayList<>();
        switch (rotations) {
            case 1 -> {
                rotateBinaryRepresentationBy90Degrees(rotatedBinaryRepresentation);
            }
            case 2 -> {
                rotatedBinaryRepresentation = binaryRepresentation;
                Collections.reverse(rotatedBinaryRepresentation);
            }
            case 3 -> {
                rotateBinaryRepresentationBy90Degrees(rotatedBinaryRepresentation);
                Collections.reverse(rotatedBinaryRepresentation);
            }
            default -> throw new AssertionError();
        }

        this.binaryRepresentation = rotatedBinaryRepresentation;
        this.orientation = newCardinal;
    }

    private void rotateBinaryRepresentationBy90Degrees(List<Boolean> rotatedBinaryRepresentation) {
        for (int i = 0; i < binaryRepresentation.size(); i++) {
            int ti;
            int imh = i % height;
            int idh = i / height;
            if (idh % 2 == 0) {
                if (i % 2 == 0)
                    ti = (imh + 1) * width - (idh + 1);
                else
                    ti = (imh + 1) * width - (width - idh);
            }
            else {
                if (i % 2 == 0)
                    ti = (height - imh) * width - (width - idh);
                else
                    ti = (height - imh) * width - (idh + 1);
            }
            rotatedBinaryRepresentation.add(binaryRepresentation.get(ti));
        }
    }

    private int cardinalValue(Cardinal cardinal) {
        return switch (cardinal) {
            case EAST  -> 0;
            case NORTH -> 1;
            case WEST  -> 2;
            case SOUTH -> 3;
            default    -> -1;
        };
    }

    // Returns how many clockwise rotations (90°) are between 'from' and 'to'.
    private int cardinalDifference(Cardinal from, Cardinal to) {
        return (cardinalValue(to) - cardinalValue(from) + 4) % 4;
    }
}
