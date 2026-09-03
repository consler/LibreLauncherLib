package net.consler.librelauncherlib.exception;

public class VersionJsonMissingException extends RuntimeException {
    public VersionJsonMissingException(String message) {
        super(message);
    }

    public VersionJsonMissingException(String message, Throwable cause) {
        super(message, cause);
    }
}
