package net.consler.librelauncherlib.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * A profile that's used to authenticate with Microsoft on launch
 * @param username Minecraft username
 * @param uuid Minecraft UUID
 * @param accessToken Microsoft access token
 * @param refreshToken Microsoft refresh token
 * @param expiresAt When {@code accessToken} stops being valid.
 */
public record AuthProfile(String username, String uuid, String accessToken, String refreshToken, Instant expiresAt)
{
    private static final Duration EXPIRY_BUFFER = Duration.ofMinutes(2);

    public static AuthProfile Offline(String username)
    {
        return new AuthProfile(username, UUID.nameUUIDFromBytes(username.getBytes()).toString(), "0", "0", Instant.MAX);
    }

    public static AuthProfile Online(String username, String uuid, String accessToken, String refreshToken, Instant expiresAt)
    {
        return new AuthProfile(username, uuid, accessToken, refreshToken, expiresAt);
    }

    /**
     * @return true if the access token is expired, really close to expire unless an offline profile.
     */
    public boolean isExpired()
    {
        if (expiresAt.equals(Instant.MAX)) return false; // offline profile
        return Instant.now().isAfter(expiresAt.minus(EXPIRY_BUFFER));
    }
}