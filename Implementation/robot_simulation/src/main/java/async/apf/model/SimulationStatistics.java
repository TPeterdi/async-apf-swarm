package async.apf.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SimulationStatistics {
    private final int robotCount;
    private final int startWidth;
    private final int startHeight;
    private int maxWidth;
    private int maxHeight;
    private int maxStepCount;

    private final List<Integer> activationCounter;
    private final List<Integer> cycleCounter;
    
    // Keys: 1st: robot index
    //     - 2nd: phase number (1-7)
    private final HashMap<Integer, HashMap<Integer, Integer>> phaseCounter;

    private Instant startTime;
    private Instant endTime;

    public SimulationStatistics(int robotCount, int startWidth, int startHeight) {
        this.robotCount = robotCount;
        this.startWidth = Math.min(startWidth, startHeight);
        this.startHeight = Math.max(startWidth, startHeight);
        this.maxWidth = this.startWidth;
        this.maxHeight = this.startHeight;
        this.maxStepCount = 0;

        this.activationCounter = new ArrayList<>(Collections.nCopies(robotCount, 0));
        this.cycleCounter = new ArrayList<>(Collections.nCopies(robotCount, 0));
        this.phaseCounter = new HashMap<>(robotCount);
        for (int i = 0; i < robotCount; i++) {
            HashMap<Integer, Integer> init = new HashMap<>();
            for (int j = 1; j <= 7; j++) {
                init.put(j, 0);
            }
            phaseCounter.put(i, init);
        }
        this.startTime = Instant.now();
    }

    public void incrementActivationCounter(int index) {
        this.activationCounter.set(index, this.activationCounter.get(index) + 1);
    }

    public void incrementCycleCounter(int index) {
        this.cycleCounter.set(index, this.cycleCounter.get(index) + 1);
    }

    public void incrementStepsForPhase(int robotIndex, int phaseNumber) {
        var robotSteps = phaseCounter.get(robotIndex);
        int newCount = robotSteps.values()
            .stream()
            .mapToInt(Integer::intValue)
            .sum() + 1;
        if (newCount > maxStepCount)
            maxStepCount = newCount;

        int phaseStepCount = robotSteps.get(phaseNumber);
        robotSteps.put(phaseNumber, phaseStepCount + 1);
    }

    public List<Integer> getStepCounts() {
        List<Integer> copy = new ArrayList<>(this.robotCount);
        for (final var phaseSteps : this.phaseCounter.values()) {
            copy.add(phaseSteps
                .values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum()
            );
        }
        return copy;
    }

    public int getStepCountForPhase(int k) {
        return phaseCounter.values()
            .stream()
            .mapToInt(robotPhases -> robotPhases.getOrDefault(k, 0))
            .sum();
    }

    public int getRobotCount() {
        return robotCount;
    }

    public int getStartWidth() {
        return startWidth;
    }

    public int getStartHeight() {
        return startHeight;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public int getMaxStepCount() {
        return maxStepCount;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant time) {
        startTime = time;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant time) {
        endTime = time;
    }

    public long getDuration() {
        if (startTime == null)
            return 0;
        if (endTime == null)
            return Duration.between(startTime, Instant.now()).toMillis();
        
        return Duration.between(startTime, endTime).toMillis();
    }

    public void trackSERSize(int a, int b) {
        int currentWidth = Math.min(a, b);
        int currentHeight = Math.max(a, b);
        this.maxWidth = Math.max(this.maxWidth, currentWidth);
        this.maxHeight = Math.max(this.maxHeight, currentHeight);
    }

    public HashMap<Integer, HashMap<Integer, Integer>> getPhaseStepCounts() {
        return phaseCounter;
    }
}