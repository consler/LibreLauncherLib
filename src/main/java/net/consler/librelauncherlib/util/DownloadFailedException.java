package net.consler.librelauncherlib.util;

import net.consler.librelauncherlib.LibraryException;

public class DownloadFailedException extends LibraryException {
    public DownloadFailedException(String message) {
        super(message);
    }

    public DownloadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
