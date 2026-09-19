package net.consler.librelauncherlib.exception;

public class TokenRefreshException extends AuthenticationException
{
    public TokenRefreshException(String message, Throwable cause)
    {
        super(message, cause);
    }
}