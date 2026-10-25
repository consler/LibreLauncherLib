package net.consler.librelauncherlib.auth;

public class UserCancelledException extends AuthenticationException
{
    public UserCancelledException(String message)
    {
        super(message);
    }
}