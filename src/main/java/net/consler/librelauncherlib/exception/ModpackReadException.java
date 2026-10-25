package net.consler.librelauncherlib.exception;

import java.io.Serial;

public class ModpackReadException extends RuntimeException
{
    @Serial
    private static final long serialVersionUID = 1L;

    public ModpackReadException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
