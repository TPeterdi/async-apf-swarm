package async.apf.model;

import java.util.Random;

public class AsyncScheduler {
    private final int robotCount;
    private final Random random = new Random();

    public AsyncScheduler(int robotCount) {
        this.robotCount = robotCount;
    }

    public int pickNext() {
        return random.nextInt(robotCount);
    }
}
