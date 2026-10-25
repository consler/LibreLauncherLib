package net.consler.librelauncherlib.modloader;

public class ModLoaderException extends RuntimeException
{
    public ModLoaderException(String message) {
        super(message);
    }

    public ModLoaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
