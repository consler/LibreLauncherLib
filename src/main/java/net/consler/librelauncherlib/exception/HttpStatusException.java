package net.consler.librelauncherlib.exception;

public class HttpStatusException extends RuntimeException {
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
