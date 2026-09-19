package net.consler.librelauncherlib.exception;

public class UserCancelledException extends AuthenticationException
{
    public UserCancelledException(String message)
    {
        super(message);
    }
}