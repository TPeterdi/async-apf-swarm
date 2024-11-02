package async.apf.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import async.apf.model.enums.Cardinal;

public final class OrientationHelper {
    private OrientationHelper() {}

    public static RobotOrientation orientRobotAndConfiguration(List<Coordinate> configuration) {
        List<Coordinate> copy = copyCoordinates(configuration);

        ConfigurationOrientation orientedConfiguration = orientConfiguration(configuration);
        Coordinate selfPosition = getSelfPosition(copy, orientedConfiguration);

        return new RobotOrientation(orientedConfiguration, selfPosition);
    }

    public static List<Coordinate> copyCoordinates(List<Coordinate> configuration) {
        List<Coordinate> copy = new ArrayList<>();
        for (Coordinate coordinate : configuration) {
            copy.add(new Coordinate(coordinate.getX(), coordinate.getY()));
        }
        return copy;
    }

    public static ConfigurationOrientation orientConfiguration(List<Coordinate> configuration) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        // Iterate over the points to find the min and max x, y values
        for (Coordinate point : configuration) {
            int x = point.getX();
            int y = point.getY();
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }

        // We are interested in grid points, so we add 1 to the height and width
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;

        Coordinate origin = new Coordinate(minX, minY);

        // Reposition configuration to the origin.
        for (Coordinate point : configuration) {
            point.translateInPlace(origin);
        }

        // Rotate the configuration such that it's a "tall" rectangle (height >= width)
        boolean rotated = false;
        if (width > height) {
            makeConfigurationTall(height, configuration);
            int tmp = height;
            height = width;
            width = tmp;
            rotated = true;
        }

        Boolean[][] positionMatrix = initializePositionMatrix(width, height, configuration);
        ConfigurationOrientation orientation = findBestOrientation(width, height, positionMatrix);
        if (rotated) {
            orientation.adjustOrientationByCardinal(Cardinal.EAST);
        }
        return orientation;
    }

    private static void makeConfigurationTall(int height, List<Coordinate> configuration) {
        Coordinate corner = new Coordinate(-height + 1, 0);
        for (Coordinate point : configuration) {
            point.rotateByCardinal(Cardinal.WEST);
            point.translateInPlace(corner);
        }
    }

    public static Coordinate indexToCoordinate(int index, int width) {
        int y = index / width;
        int rem = index % width;
        int x = y % 2 == 0
            ? rem
            : width - rem - 1;
        return new Coordinate(x, y);
    }

    public static int coordinateToIndex(Coordinate index, int width) {
        int xOffset = index.getY() % 2 == 0
            ? index.getX() 
            : width - index.getX() - 1;

        return index.getY() * width + xOffset;
    }

    private static Boolean[][] initializePositionMatrix(int width, int height, List<Coordinate> configuration) {
        // Create a boolean matrix to determine lexicographic strings
        Boolean[][] positionMatrix = new Boolean[width][height];
        for (Boolean[] positionMatrixColumn : positionMatrix) {
            for (int y = 0; y < positionMatrixColumn.length; y++) {
                positionMatrixColumn[y] = false;
            }
        }
        for (Coordinate point : configuration) {
            positionMatrix[point.getX()][point.getY()] = true;
        }
        return positionMatrix;
    }

    private static ConfigurationOrientation findBestOrientation(int width, int height, Boolean[][] positionMatrix) {
        if (width > 1) {
            if (height > width) {
                // lamAB lamBA lamCD lamDC
                ConfigurationOrientation lamABOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true, true, true),
                    Cardinal.NORTH,
                    false,
                    width,
                    height);
    
                ConfigurationOrientation lamBAOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    false, true, true),
                    Cardinal.NORTH,
                    true,
                    width,
                    height);

                ConfigurationOrientation lamCDOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    false, false, true),
                    Cardinal.SOUTH,
                    false,
                    width,
                    height);
    
                ConfigurationOrientation lamDCOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true, false, true),
                    Cardinal.SOUTH,
                    true,
                    width,
                    height);
                
                return findOrientationWithLargestLexographicBinaryString(new ArrayList<>(List.of(
                    lamABOrientation,
                    lamBAOrientation,
                    lamCDOrientation,
                    lamDCOrientation
                )));
            }
            // width == height (square) && width > 1
            else {
                // lamAB lamBA lamAD lamDA lamBC lamCB lamDC lamCD
                ConfigurationOrientation lamABOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true, true, true),
                    Cardinal.NORTH,
                    false,
                    width,
                    height);
    
                ConfigurationOrientation lamBAOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    false, true, true),
                    Cardinal.NORTH,
                    true,
                    width,
                    height);
    
                ConfigurationOrientation lamADOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true,  true,  false),
                    Cardinal.EAST,
                    false,
                    width,
                    height);
    
                ConfigurationOrientation lamDAOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true,  false, false),
                    Cardinal.EAST,
                    true,
                    width,
                    height);
    
                ConfigurationOrientation lamBCOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    false, true,  false),
                    Cardinal.WEST,
                    false,
                    width,
                    height);
    
                ConfigurationOrientation lamCBOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    false, false, false),
                    Cardinal.WEST,
                    true,
                    width,
                    height);
    
                ConfigurationOrientation lamDCOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true, false, true),
                    Cardinal.SOUTH,
                    true,
                    width,
                    height);
    
                ConfigurationOrientation lamCDOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    false, false, true),
                    Cardinal.SOUTH,
                    false,
                    width,
                    height);
                
                return findOrientationWithLargestLexographicBinaryString(new ArrayList<>(List.of(
                    lamABOrientation,
                    lamBAOrientation,
                    lamADOrientation,
                    lamDAOrientation,
                    lamBCOrientation,
                    lamCBOrientation,
                    lamDCOrientation,
                    lamCDOrientation
                )));
            }
        }
        // width == 1
        else {
            // lamAB lamCD
            ConfigurationOrientation lamABOrientation = new ConfigurationOrientation(
                snakeIterate(positionMatrix, x -> x, width, height,
                true, true, true),
                Cardinal.NORTH,
                false,
                width,
                height);

            ConfigurationOrientation lamCDOrientation = new ConfigurationOrientation(
                snakeIterate(positionMatrix, x -> x, width, height,
                false, false, true),
                Cardinal.SOUTH,
                false,
                width,
                height);
            
            return findOrientationWithLargestLexographicBinaryString(new ArrayList<>(List.of(
                lamABOrientation,
                lamCDOrientation
            )));
        }
    }

    public static <T, R> List<R> snakeIterate(T[][] matrix, Function<T, R> function, int width, int height, boolean leftToRight, boolean bottomToTop, boolean weaveHorizontally) {
        List<R> result = new ArrayList<>();
        boolean flip = false;

        for (    int outer = 0; outer < (weaveHorizontally ? height : width); outer++) {
            for (int inner = 0; inner < (weaveHorizontally ? width : height); inner++) {
                int x = findX(outer, inner, width,  flip, weaveHorizontally, leftToRight);
                int y = findY(outer, inner, height, flip, weaveHorizontally, bottomToTop);
        
                result.add(function.apply(matrix[x][y]));
            }
            flip = !flip;
        }

        return result;
    }

    private static int findX(int outer, int inner, int width, boolean flip, boolean weaveHorizontally, boolean leftToRight) {
        // It just works (TM)
        return weaveHorizontally
            ? (leftToRight == flip ? width - inner - 1 : inner)
            : (leftToRight ? outer : width - outer - 1);
    }
    private static int findY(int outer, int inner, int height, boolean flip, boolean weaveHorizontally, boolean bottomToTop) {
        // It just works (TM)
        return weaveHorizontally
            ? (bottomToTop ? outer : height - outer - 1)
            : (bottomToTop == flip ? height - inner - 1 : inner);
    }
    
    private static ConfigurationOrientation findOrientationWithLargestLexographicBinaryString(ArrayList<ConfigurationOrientation> orientations) {
        if (orientations == null || orientations.isEmpty()) {
            throw new IllegalArgumentException("The input array cannot be null or empty.");
        }

        // Assumes all lists are of the same size
        int binaryLength = orientations.get(0).getBinaryRepresentation().size();

        for (int index = 0; index < binaryLength; index++) {
            if (orientations.size() <= 1) break; // If one or no candidates remain, break out of the loop
            final int finalIndex = index;
            boolean hasTrueAtIndex = orientations
                .stream()
                .anyMatch(orientation -> Boolean.TRUE.equals(orientation.getBinaryRepresentation().get(finalIndex)));

            if (hasTrueAtIndex) {
                // Check all candidates at the current index
                for (int i = 0; i < orientations.size(); i++) {
                    List<Boolean> binaryList = orientations.get(i).getBinaryRepresentation();
                    if (Boolean.FALSE.equals(binaryList.get(index))) {
                        orientations.remove(i);
                        i--;
                    }
                }
            }
        }

        return orientations.get(0);
    }

    private static Coordinate getSelfPosition(List<Coordinate> copy, ConfigurationOrientation orientedConfiguration) {
        Coordinate selfPosition = null;
        for (Coordinate coordinate : copy) {
            if (coordinate.getX() == 0 && coordinate.getY() == 0) {
                selfPosition = coordinate;
                continue;
            }
            
            if (orientedConfiguration.isXMirrored()) {
                coordinate.setX(-coordinate.getX());
            }
            coordinate.counterRotateByCardinal(orientedConfiguration.getOrientation());
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        for (Coordinate coordinate : copy) {
            minX = Math.min(coordinate.getX(), minX);
            minY = Math.min(coordinate.getY(), minY);
        }
        // selfPosition should never be actually null here.
        selfPosition.setX(-minX);
        selfPosition.setY(-minY);
        return selfPosition;
    }

    public static boolean isSymmetric(List<Coordinate> coordinates) {
        Set<Coordinate> coords = new HashSet<>(coordinates);
        
        int minX = coordinates.stream().mapToInt(Coordinate::getX).min().orElse(0);
        int maxX = coordinates.stream().mapToInt(Coordinate::getX).max().orElse(0);
        int minY = coordinates.stream().mapToInt(Coordinate::getY).min().orElse(0);
        int maxY = coordinates.stream().mapToInt(Coordinate::getY).max().orElse(0);

        int midX = (minX + maxX) / 2;
        int midY = (minY + maxY) / 2;

        return hasVerticalSymmetry(coords, midX)
            || hasHorizontalSymmetry(coords, midY)
            || hasDiagonalSymmetry1(coords, midX, midY)
            || hasDiagonalSymmetry2(coords, midX, midY)
            || has180DegreeRotationSymmetry(coords, midX, midY);
    }

    public static boolean hasVerticalSymmetry(Set<Coordinate> coords, int midX) {
        for (Coordinate coord : coords) {
            Coordinate mirrored = new Coordinate(2 * midX - coord.getX(), coord.getY());
            if (!coords.contains(mirrored)) return false;
        }
        return true;
    }

    public static boolean hasHorizontalSymmetry(Set<Coordinate> coords, int midY) {
        for (Coordinate coord : coords) {
            Coordinate mirrored = new Coordinate(coord.getX(), 2 * midY - coord.getY());
            if (!coords.contains(mirrored)) return false;
        }
        return true;
    }

    public static boolean hasDiagonalSymmetry1(Set<Coordinate> coords, int midX, int midY) {
        for (Coordinate coord : coords) {
            Coordinate mirrored = new Coordinate(midX - (coord.getY() - midY), midY - (midX - coord.getX()));
            if (!coords.contains(mirrored)) return false;
        }
        return true;
    }

    public static boolean hasDiagonalSymmetry2(Set<Coordinate> coords, int midX, int midY) {
        for (Coordinate coord : coords) {
            Coordinate mirrored = new Coordinate(midX + (coord.getY() - midY), midY + (midX - coord.getX()));
            if (!coords.contains(mirrored)) return false;
        }
        return true;
    }

    public static boolean has180DegreeRotationSymmetry(Set<Coordinate> coords, int midX, int midY) {
        for (Coordinate coord : coords) {
            Coordinate rotated = new Coordinate(2 * midX - coord.getX(), 2 * midY - coord.getY());
            if (!coords.contains(rotated)) return false;
        }
        return true;
    }
}