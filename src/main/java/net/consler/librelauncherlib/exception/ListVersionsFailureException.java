package net.consler.librelauncherlib.exception;

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
