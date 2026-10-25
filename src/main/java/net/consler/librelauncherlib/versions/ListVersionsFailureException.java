package net.consler.librelauncherlib.versions;

public class ListVersionsFailureException extends RuntimeException
{
    public ListVersionsFailureException(String message)
    {
        super(message);
    }

    public ListVersionsFailureException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
