package net.consler.librelauncherlib.launch;

import net.consler.librelauncherlib.LibraryException;

public class LaunchException extends LibraryException {
    public LaunchException(String message) {
        super(message);
    }

    public LaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
