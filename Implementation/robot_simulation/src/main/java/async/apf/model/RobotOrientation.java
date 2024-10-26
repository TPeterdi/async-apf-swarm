package async.apf.model;

public class RobotOrientation extends ConfigurationOrientation {
    private final Coordinate selfPosition;
    private Coordinate headRobotPosition;
    private Coordinate tailRobotPosition;

    public RobotOrientation(ConfigurationOrientation config, Coordinate selfPosition) {
        super(config.getBinaryRepresentation(), config.getOrientation(), config.isXMirrored(), config.getWidth(), config.getHeight());
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
    
    public void setHeadRobotPosition(Coordinate headRobotPosition) {
        this.headRobotPosition = headRobotPosition;
    }

    public void setTailRobotPosition(Coordinate tailRobotPosition) {
        this.tailRobotPosition = tailRobotPosition;
    }
}
