package net.consler.librelauncherlib.install;

import net.consler.librelauncherlib.LibraryException;

public class InstallationException extends LibraryException {
    public InstallationException(String message) {
        super(message);
    }

    public InstallationException(String message, Throwable cause) {
        super(message, cause);
    }
}
