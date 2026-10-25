package net.consler.librelauncherlib.integration.modrinth;

import java.io.Serial;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * The only exception this wrapper throws. Unchecked, because it travels through CompletableFutures
 * (where it arrives wrapped in a CompletionException, see {@link #unwrap(Throwable)}).
 * <p>
 * Use {@link #getKind()} to tell failures apart:
 * <pre>
 * switch (e.getKind())
 * {
 *     case NOT_FOUND    -> ...  // 404
 *     case RATE_LIMITED -> ...  // 429, see getRetryAfterSeconds()
 *     case API_ERROR    -> ...  // any other non-200, see getStatusCode() / getDescription()
 *     case NETWORK      -> ...  // no connection, timeout, interrupted
 *     case PARSE        -> ...  // 200 but empty / unexpected body
 * }
 * </pre>
 */
public class ModrinthException extends RuntimeException
{
    public enum Kind
    {
        NOT_FOUND, RATE_LIMITED, API_ERROR, NETWORK, PARSE
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private final Kind kind;
    private final int statusCode;
    private final String error;
    private final String description;
    private final String responseBody;
    private final long retryAfterSeconds;

    private ModrinthException(Kind kind, String message, Throwable cause, int statusCode,
                              String error, String description, String responseBody, long retryAfterSeconds)
    {
        super(message, cause);
        this.kind = kind;
        this.statusCode = statusCode;
        this.error = error;
        this.description = description;
        this.responseBody = responseBody;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /** Creates an exception for a network failure. */
    public static ModrinthException network(String message, Throwable cause)
    {
        return new ModrinthException(Kind.NETWORK, message, cause, 0, null, null, null, -1);
    }

    /** Creates an exception for an invalid or unexpected API response. */
    public static ModrinthException parse(String message, String responseBody, Throwable cause)
    {
        return new ModrinthException(Kind.PARSE, message, cause, 0, null, null, responseBody, -1);
    }

    /** Creates an exception from an HTTP error response. */
    public static ModrinthException api(int statusCode, String error, String description, String responseBody, long retryAfterSeconds)
    {
        Kind kind = switch (statusCode)
        {
            case 404 -> Kind.NOT_FOUND;
            case 429 -> Kind.RATE_LIMITED;
            default -> Kind.API_ERROR;
        };

        String message = "Modrinth API error " + statusCode
                + (error != null ? " (" + error + ")" : "")
                + (description != null ? ": " + description : "");

        return new ModrinthException(kind, message, null, statusCode, error, description, responseBody, retryAfterSeconds);
    }

    public Kind getKind() { return kind; }

    /** @return The HTTP status code, or 0 if there was no usable HTTP response (NETWORK, PARSE). */
    public int getStatusCode() { return statusCode; }

    /** @return Modrinth's error name, e.g. "invalid_input", or null. */
    public String getError() { return error; }

    /** @return Modrinth's error description, or null. */
    public String getDescription() { return description; }

    /** @return The raw response body, or null if there was none. */
    public String getResponseBody() { return responseBody; }

    /** @return Seconds to wait before retrying (RATE_LIMITED only), or -1 if the response didn't say. */
    public long getRetryAfterSeconds() { return retryAfterSeconds; }

    public boolean isNotFound() { return kind == Kind.NOT_FOUND; }
    public boolean isRateLimited() { return kind == Kind.RATE_LIMITED; }
    public boolean isNetworkError() { return kind == Kind.NETWORK; }

    /**
     * Strips the CompletionException / ExecutionException wrappers CompletableFuture adds.
     * Use it on the throwable you get from join(), get() or exceptionally().
     */
    public static Throwable unwrap(Throwable throwable)
    {
        while ((throwable instanceof CompletionException || throwable instanceof ExecutionException)
                && throwable.getCause() != null)
        {
            throwable = throwable.getCause();
        }
        return throwable;
    }
}