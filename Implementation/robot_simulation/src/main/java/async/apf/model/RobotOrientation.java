package async.apf.model;

public class RobotOrientation extends ConfigurationOrientation {
    private final Coordinate selfPosition;

    public RobotOrientation(ConfigurationOrientation config, Coordinate selfPosition) {
        super(config.getBinaryRepresentation(), config.getOrientation(), config.isXMirrored(), config.getWidth(), config.getHeight());
        this.selfPosition = selfPosition;
    }

    public Coordinate getSelfPosition() {
        return selfPosition;
    }
}
