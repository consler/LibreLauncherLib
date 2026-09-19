package net.consler.librelauncherlib.auth;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Holds an {@link AuthProfile} and refreshes it lazily, only when someone actually asks for the
 * access token and the current one is expired or close to expiring.
 */
public class AuthSession
{
    private final List<Consumer<AuthProfile>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Throwable>> errorListeners = new CopyOnWriteArrayList<>();

    private volatile AuthProfile profile;
    private CompletableFuture<AuthProfile> inFlightRefresh;

    public AuthSession(AuthProfile initialProfile)
    {
        this.profile = initialProfile;
    }

    /** @return the most recently issued profile, without triggering a refresh check. */
    public AuthProfile getProfile()
    {
        return profile;
    }

    /** Registers a listener called with the new profile every time a refresh succeeds — e.g. to persist it to disk. */
    public void onRefresh(Consumer<AuthProfile> listener)
    {
        listeners.add(listener);
    }

    /** Registers a listener called if a refresh fails — e.g. to prompt the user to log in again. */
    public void onRefreshError(Consumer<Throwable> listener)
    {
        errorListeners.add(listener);
    }

    /**
     *  Returns a valid AuthProfile, refreshing first if needed.
     */
    public synchronized CompletableFuture<AuthProfile> ensureFreshProfile(MicrosoftAuthenticator authenticator)
    {
        if (!profile.isExpired()) return CompletableFuture.completedFuture(profile);

        if (inFlightRefresh == null)
        {
            inFlightRefresh = authenticator.loginWithRefreshToken(profile.refreshToken())
                    .whenComplete((newProfile, error) ->
                    {
                        synchronized (this)
                        {
                            inFlightRefresh = null;
                            if (newProfile != null) profile = newProfile;
                        }

                        if (error != null) errorListeners.forEach(l -> l.accept(error));
                        else listeners.forEach(l -> l.accept(newProfile));
                    });
        }

        return inFlightRefresh;
    }
}