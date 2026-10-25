package net.consler.librelauncherlib.util;

import net.consler.librelauncherlib.LibraryException;

public class HttpStatusException extends LibraryException {
    private final int statusCode;

    public HttpStatusException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
