package net.consler.librelauncherlib.instance;

public class ResourcePackReadException extends RuntimeException
{
    public ResourcePackReadException(String message) {
        super(message);
    }

    public ResourcePackReadException(String message, Throwable cause) {
        super(message, cause);
    }
}
