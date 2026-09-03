package net.consler.librelauncherlib.exception;

public class VersionNotFoundException extends RuntimeException {
    public VersionNotFoundException(String message) {
        super(message);
    }

    public VersionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
