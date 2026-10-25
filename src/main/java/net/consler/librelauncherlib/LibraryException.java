package net.consler.librelauncherlib;

public class LibraryException extends RuntimeException
{
    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
