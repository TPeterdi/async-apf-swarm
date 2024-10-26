package async.apf.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import async.apf.model.enums.Cardinal;

public final class OrientationHelper {
    private OrientationHelper() {}

    public static RobotOrientation orientRobotAndConfiguration(List<Coordinate> configuration) {
        Coordinate selfPosition = null;

        // Iterate over the points to find the min and max x, y values
        for (Coordinate point : configuration) {
            if (point.getX() == 0 && point.getY() == 0) {
                selfPosition = point;
                break;
            }
        }

        RobotOrientation orientation = new RobotOrientation(orientPattern(configuration), selfPosition);
        findHeadAndTailCoordinates(orientation);

        return orientation;
    }

    public static ConfigurationOrientation orientPattern(List<Coordinate> configuration) {
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

        // Rotate the configuration such that it's a "tall" rectangle (height >= width)
        if (width > height) {
            for (Coordinate point : configuration) {
                point.rotateByCardinal(Cardinal.WEST);
            }
            int tmp = width;
            width = height;
            height = tmp;

            tmp = maxX;
            maxX = -minY;
            minY = minX;
            minX = -maxY;
            maxY = tmp;
        }

        Coordinate origin = new Coordinate(minX, minY);

        // Reposition configuration to the origin.
        for (Coordinate point : configuration) {
            point.translateInPlace(origin);
        }

        Boolean[][] positionMatrix = initializePositionMatrix(width, height, configuration);

        ConfigurationOrientation orientation = findBestOrientation(width, height, positionMatrix);

        // Transform configuration according to the best orientation.
        Cardinal cardinalOrientation = orientation.getOrientation();
        for (Coordinate point : configuration) {
            point.rotateByCardinal(cardinalOrientation);
            switch (cardinalOrientation) {
                case WEST ->  point.translateInPlace(new Coordinate(-maxY, 0));
                case SOUTH -> point.translateInPlace(new Coordinate(-maxX, -maxY));
                case EAST ->  point.translateInPlace(new Coordinate(0, -maxX));
                default -> {
                }
            }
            if (orientation.isXMirrored()) {
                point.setX(width - point.getX());
            }
        }

        return orientation;
    }

    private static void findHeadAndTailCoordinates(RobotOrientation orientation) {
        List<Boolean> binaryValue = orientation.getBinaryRepresentation();
        Coordinate headCoordinate = null;
        Coordinate tailCoordinate = null;
        int index = 0;
        int binaryLength = binaryValue.size();

        while (index < binaryLength && (headCoordinate == null || tailCoordinate == null)) {
            if (headCoordinate == null && Boolean.TRUE.equals(binaryValue.get(index))) {
                int y = binaryLength / orientation.getWidth();
                int rem = binaryLength % orientation.getWidth();
                int x = y % 2 == 0
                    ? orientation.getWidth() - rem - 1
                    : rem;
                headCoordinate = new Coordinate(x, y);
            }
            if (tailCoordinate == null && Boolean.TRUE.equals(binaryValue.get(binaryLength - index - 1))) {
                int y = binaryLength / orientation.getWidth();
                int rem = binaryLength % orientation.getWidth();
                int x = y % 2 == 0
                    ? orientation.getWidth() - rem - 1
                    : rem;
                tailCoordinate = new Coordinate(orientation.getWidth() - x - 1, orientation.getHeight() - y - 1);
            }
            index++;
        }

        orientation.setHeadRobotPosition(headCoordinate);
        orientation.setTailRobotPosition(tailCoordinate);
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
    
                ConfigurationOrientation lamDCOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true, false, true),
                    Cardinal.SOUTH,
                    false,
                    width,
                    height);
                
                return findOrientationWithLargestLexographicBinaryString(Arrays.asList(
                    lamABOrientation,
                    lamBAOrientation,
                    lamCDOrientation,
                    lamDCOrientation
                ));
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
                    false,
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
                    false,
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
                    false,
                    width,
                    height);
    
                ConfigurationOrientation lamDCOrientation = new ConfigurationOrientation(
                    snakeIterate(positionMatrix, x -> x, width, height,
                    true, false, true),
                    Cardinal.SOUTH,
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
                
                return findOrientationWithLargestLexographicBinaryString(Arrays.asList(
                    lamABOrientation,
                    lamBAOrientation,
                    lamADOrientation,
                    lamDAOrientation,
                    lamBCOrientation,
                    lamCBOrientation,
                    lamDCOrientation,
                    lamCDOrientation
                ));
            }
        }
        // width == 1
        else {
            // lamAB lamBA
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
                false,
                width,
                height);
            
            return findOrientationWithLargestLexographicBinaryString(Arrays.asList(
                lamABOrientation,
                lamBAOrientation
            ));
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
    
    private static ConfigurationOrientation findOrientationWithLargestLexographicBinaryString(List<ConfigurationOrientation> orientations) {
        if (orientations == null || orientations.isEmpty()) {
            throw new IllegalArgumentException("The input array cannot be null or empty.");
        }

        // Assumes all lists are of the same size
        int binaryLength = orientations.get(0).getBinaryRepresentation().size();

        for (int index = 0; index < binaryLength; index++) {
            if (orientations.size() <= 1) break; // If one or no candidates remain, break out of the loop

            // Check all candidates at the current index
            for (int i = 0; i < orientations.size(); i++) {
                List<Boolean> binaryList = orientations.get(i).getBinaryRepresentation();
                if (Boolean.FALSE.equals(binaryList.get(index))) {
                    orientations.remove(i);
                }
            }
        }

        return orientations.get(0);
    }
}
