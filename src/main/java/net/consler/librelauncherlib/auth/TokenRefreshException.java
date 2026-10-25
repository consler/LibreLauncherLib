package net.consler.librelauncherlib.auth;

public class TokenRefreshException extends AuthenticationException
{
    public TokenRefreshException(String message, Throwable cause)
    {
        super(message, cause);
    }
}