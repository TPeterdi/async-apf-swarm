package async.apf.model.exceptions;

public class InvalidInputException extends Exception {
    public InvalidInputException() {
        super("Starting configuration and target pattern must have the same amount of coordinates!");
    }
}
