package async.apf.model;

public class RobotOrientation extends ConfigurationOrientation {
    private final Coordinate selfPosition;
    private Coordinate headRobotPosition;
    private Coordinate tailRobotPosition;

    public RobotOrientation(ConfigurationOrientation config, Coordinate selfPosition) {
        super(config.getConfigurationBinaryString(), config.getOrientation(), config.isXMirrored());
        this.selfPosition = selfPosition;
    }

    public Coordinate getSelfPosition() {
        return selfPosition;
    }

    public Coordinate getHeadRobotPosition() {
        return headRobotPosition;
    }

    public Coordinate getTailRobotPosition() {
        return tailRobotPosition;
    }
}
