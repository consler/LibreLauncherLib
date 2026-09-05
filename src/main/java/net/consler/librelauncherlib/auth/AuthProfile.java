package net.consler.librelauncherlib.auth;

import java.util.UUID;

public record AuthProfile(String username, String uuid, String accessToken)
{
    public static AuthProfile Offline(String username)
    {
        return new AuthProfile(username, UUID.nameUUIDFromBytes(username.getBytes()).toString(), "0");
    }
    public static AuthProfile Online(String username, String uuid, String accessToken)
    {
        return new AuthProfile(username, uuid, accessToken);
    }
}
