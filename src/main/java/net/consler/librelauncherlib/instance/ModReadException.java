package net.consler.librelauncherlib.instance;

public class ModReadException extends RuntimeException
{
    public ModReadException(String message)
    {
        super(message);
    }

    public ModReadException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
