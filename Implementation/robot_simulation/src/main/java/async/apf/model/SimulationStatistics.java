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
    private final List<Integer> stepCounter;
    
    // Keys: 1st: phase number (1-7)
    //     - 2nd: robot index
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
        this.stepCounter = new ArrayList<>(Collections.nCopies(robotCount, 0));
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
        this.activationCounter.set(index, this.stepCounter.get(index) + 1);
    }

    public void incrementCycleCounter(int index) {
        this.cycleCounter.set(index, this.stepCounter.get(index) + 1);
    }

    public void incrementStepCounter(int index) {
        int count = this.stepCounter.get(index) + 1;
        if (count > maxStepCount)
            maxStepCount = count;
        this.stepCounter.set(index, count);
    }

    public void incrementPhaseCounter(int robotIndex, int phaseNumber) {
        int phaseCount = phaseCounter.get(robotIndex).get(phaseNumber);
        phaseCounter.get(robotIndex).put(phaseNumber, phaseCount + 1);
    }

    public List<Integer> getStepCounts() {
        List<Integer> copy = new ArrayList<>(this.robotCount);
        for (int count : this.stepCounter) {
            copy.add(count);
        }
        return copy;
    }

    public int getStepCountForPhase(int k) {
        return phaseCounter.get(k)
            .values()
            .stream()
            .mapToInt(Integer::intValue)
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