package fr.bananasmoothii.scriptcommands.core.configs_storage;

public class InvalidConfigException extends RuntimeException {
    public InvalidConfigException(String message) {
        super(message + "\nThe plugin will shut down, if you want to regenerate a new working configuration file, delete or rename the current one.");
    }
    public InvalidConfigException(String message, Throwable cause) {
        super(message + "\nThe plugin will shut down, if you want to regenerate a new working configuration file, delete or rename the current one.", cause);
    }
}
