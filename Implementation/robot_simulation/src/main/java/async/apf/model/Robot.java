package async.apf.model;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import async.apf.model.enums.Cardinal;
import async.apf.model.enums.RobotEventType;
import async.apf.model.events.RobotEvent;

public class Robot {
    private boolean active = false;

    private CountDownLatch lookLatch;

    private RobotOrientation currentOrientation;
    private List<Coordinate> currentConfiguration;
    private List<Coordinate> targetPattern;
    private Cardinal nextMove = null;

    public CompletableFuture<RobotEvent> activate() {
        if (this.active) return CompletableFuture.completedFuture(new RobotEvent(RobotEventType.ACTIVE));

        this.active = true;
        this.lookLatch = new CountDownLatch(1);
        // LOOK
        return CompletableFuture.supplyAsync(() -> {
            return new RobotEvent(RobotEventType.LOOK);
        })
        // COMPUTE
        .thenCompose(event ->
            CompletableFuture.supplyAsync(() -> {
                try {
                    lookLatch.await();

                    // TODO: Do actual computation
                    Thread.sleep((long) (Math.random() * 1000));
                    this.nextMove = Cardinal.EAST;

                    return new RobotEvent(RobotEventType.COMPUTE);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            })
        )
        // MOVE
        .thenCompose(event ->
            CompletableFuture.supplyAsync(() -> {
                switch (this.nextMove) {
                    case NORTH -> {
                        return new RobotEvent(RobotEventType.MOVE_NORTH);
                    }
                    case EAST -> {
                        return new RobotEvent(RobotEventType.MOVE_EAST);
                    }
                    case SOUTH -> {
                        return new RobotEvent(RobotEventType.MOVE_SOUTH);
                    }
                    case WEST -> {
                        return new RobotEvent(RobotEventType.MOVE_WEST);
                    }
                    default -> throw new AssertionError();
                }
            })
        )
        // IDLE
        .thenCompose(event -> 
            CompletableFuture.supplyAsync(() -> {
                this.nextMove = null;
                this.currentConfiguration = null;
                return new RobotEvent(RobotEventType.IDLE);
            })
        );
    }

    public void supplyConfigurations(List<Coordinate> relativeConfiguration, List<Coordinate> targetPattern) {
        this.currentOrientation = orientRobotAndConfiguration(relativeConfiguration);
        this.currentConfiguration = relativeConfiguration;
        this.targetPattern = targetPattern;
        lookLatch.countDown();
    }

    private static RobotOrientation orientRobotAndConfiguration(List<Coordinate> configuration) {
        Coordinate selfPosition = null;

        // Iterate over the points to find the min and max x, y values
        for (Coordinate point : configuration) {
            if (point.getX() == 0 && point.getY() == 0) {
                selfPosition = point;
                break;
            }
        }

        RobotOrientation orientation = new RobotOrientation(orientConfiguration(configuration), selfPosition);

        // TODO: Detertmine head and tail positions

        return orientation;
    }

    private static ConfigurationOrientation orientConfiguration(List<Coordinate> configuration) {
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

        boolean[][] positionMatrix = initializePositionMatrix(width, height, configuration);

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

    private static boolean[][] initializePositionMatrix(int width, int height, List<Coordinate> configuration) {
        // Create a boolean matrix to determine lexicographic strings
        boolean[][] positionMatrix = new boolean[width][height];
        for (boolean[] positionMatrixColumn : positionMatrix) {
            for (int y = 0; y < positionMatrixColumn.length; y++) {
                positionMatrixColumn[y] = false;
            }
        }
        for (Coordinate point : configuration) {
            positionMatrix[point.getX()][point.getY()] = true;
        }
        return positionMatrix;
    }

    private static ConfigurationOrientation findBestOrientation(int width, int height, boolean[][] positionMatrix) {
        if (width > 1) {
            if (height > width) {
                // lamAB lamBA lamCD lamDC
                ConfigurationOrientation lamABOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    true, true, true),
                    Cardinal.NORTH,
                    false);
    
                ConfigurationOrientation lamBAOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    false, true, true),
                    Cardinal.NORTH,
                    true);
    
                ConfigurationOrientation lamCDOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    false, false, true),
                    Cardinal.SOUTH,
                    false);
    
                ConfigurationOrientation lamDCOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    true, false, true),
                    Cardinal.SOUTH,
                    true);
                
                return findOrientationWithLargestLexographicBinaryString(new ConfigurationOrientation[]{
                    lamABOrientation,
                    lamBAOrientation,
                    lamCDOrientation,
                    lamDCOrientation,
                });
            }
            // width == height (square) && width > 1
            else {
                // lamAB lamBA lamAD lamDA lamBC lamCB lamDC lamCD
                ConfigurationOrientation lamABOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    true, true, true),
                    Cardinal.NORTH,
                    false);
    
                ConfigurationOrientation lamBAOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    false, true, true),
                    Cardinal.NORTH,
                    true);
    
                ConfigurationOrientation lamADOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    true,  true,  false),
                    Cardinal.EAST,
                    false);
    
                ConfigurationOrientation lamDAOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    true,  false, false),
                    Cardinal.EAST,
                    false);
    
                ConfigurationOrientation lamBCOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    false, true,  false),
                    Cardinal.WEST,
                    false);
    
                ConfigurationOrientation lamCBOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    false, false, false),
                    Cardinal.WEST,
                    true);
    
                ConfigurationOrientation lamDCOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    true, false, true),
                    Cardinal.SOUTH,
                    true);
    
                ConfigurationOrientation lamCDOrientation = new ConfigurationOrientation(
                    determineLexicographicString(positionMatrix, width, height,
                    false, false, true),
                    Cardinal.SOUTH,
                    false);
                
                return findOrientationWithLargestLexographicBinaryString(new ConfigurationOrientation[]{
                    lamABOrientation,
                    lamBAOrientation,
                    lamADOrientation,
                    lamDAOrientation,
                    lamBCOrientation,
                    lamCBOrientation,
                    lamDCOrientation,
                    lamCDOrientation,
                });
            }
        }
        // width == 1
        else {
            // lamAB lamBA
            ConfigurationOrientation lamABOrientation = new ConfigurationOrientation(
                determineLexicographicString(positionMatrix, width, height,
                true, true, true),
                Cardinal.NORTH,
                false);

            ConfigurationOrientation lamBAOrientation = new ConfigurationOrientation(
                determineLexicographicString(positionMatrix, width, height,
                false, true, true),
                Cardinal.NORTH,
                true);
            
            return findOrientationWithLargestLexographicBinaryString(new ConfigurationOrientation[]{
                lamABOrientation,
                lamBAOrientation,
            });
        }
    }

    private static String determineLexicographicString(boolean[][] positionMatrix, int width, int height, boolean leftToRight, boolean bottomToTop, boolean horizontalFirst) {
        boolean flip = false;
        String result = "";

        for (int outer = 0; outer < (horizontalFirst ? height : width); outer++) {
            for (int inner = 0; inner < (horizontalFirst ? width : height); inner++) {
                int x = horizontalFirst
                    ? (leftToRight == flip ? width - inner - 1 : inner)
                    : (leftToRight ? outer : width - outer - 1);
        
                int y = horizontalFirst
                    ? (bottomToTop ? outer : height - outer - 1)
                    : (bottomToTop == flip ? height - inner - 1 : inner);
        
                result += positionMatrix[x][y] ? "1" : "0";
            }
            flip = !flip;
        }

        return result;
    }

    private static ConfigurationOrientation findOrientationWithLargestLexographicBinaryString(ConfigurationOrientation[] orientations) {
        if (orientations == null || orientations.length == 0) {
            throw new IllegalArgumentException("The input array cannot be null or empty.");
        }
        
        // Initialize with the first string as the largest
        ConfigurationOrientation largest = orientations[0];
        
        // Iterate through the remaining strings to find the largest
        for (int i = 1; i < orientations.length; i++) {
            if (orientations[i].getConfigurationBinaryString().compareTo(largest.getConfigurationBinaryString()) > 0) {
                largest = orientations[i];  // Update if the current string is larger
            }
        }
        
        return largest;
    }
}