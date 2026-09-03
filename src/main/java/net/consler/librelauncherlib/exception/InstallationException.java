package net.consler.librelauncherlib.exception;

public class InstallationException extends LibraryException {
    public InstallationException(String message) {
        super(message);
    }

    public InstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
