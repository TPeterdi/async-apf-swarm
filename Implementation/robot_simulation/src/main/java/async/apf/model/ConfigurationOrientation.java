package async.apf.model;

import async.apf.model.enums.Cardinal;

public class ConfigurationOrientation {
    private final String configurationBinaryString;
    private final Cardinal orientation;
    private final boolean xMirrored;

    public ConfigurationOrientation(String configurationBinaryString, Cardinal orientation, boolean xMirrored) {
        this.configurationBinaryString = configurationBinaryString;
        this.orientation = orientation;
        this.xMirrored = xMirrored;
    }
    
    public String getConfigurationBinaryString() {
        return configurationBinaryString;
    }

    public Cardinal getOrientation() {
        return orientation;
    }

    public boolean isXMirrored() {
        return xMirrored;
    }
}
