package net.consler.librelauncherlib.auth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.consler.librelauncherlib.exception.AuthenticationException;
import net.consler.librelauncherlib.exception.TokenRefreshException;
import net.consler.librelauncherlib.exception.UserCancelledException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MicrosoftAuthenticator
{
    private static final String AUTH_URL = "https://login.live.com/oauth20_authorize.srf";
    private static final String TOKEN_URL = "https://login.live.com/oauth20_token.srf";
    private static final String REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf";
    private static final String DEVICE_TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String CLIENT_ID = "000000004C12AE6F";
    private static final String SCOPE = "XboxLive.signin offline_access";
    private static final long DEFAULT_TOKEN_LIFETIME_SECONDS = 86400L;

    private final Gson gson = new Gson();

    private final Map<String, CompletableFuture<AuthProfile>> refreshesInFlight = new ConcurrentHashMap<>();

    public record DeviceCodePrompt(String userCode, String verificationUri, String message) {}

    /**
     * Log in with Microsoft
     * @param codeProvider The code provider, usually from a WebViewFrame
     * @return A CompletableFuture that will complete with an AuthProfile
     */
    public CompletableFuture<AuthProfile> login(AuthCodeProvider codeProvider)
    {
        CookieHandler.setDefault(new CookieManager());

        String loginUrl = String.format("%s?client_id=%s&redirect_uri=%s&scope=%s&response_type=code",
                AUTH_URL, CLIENT_ID, encode(REDIRECT_URL), encode(SCOPE));

        return codeProvider.getAuthCode(loginUrl)
                .thenApply(this::extractAuthCode)
                .thenApplyAsync(this::exchangeCodeForMsTokens)
                .thenApplyAsync(this::authenticateMinecraft);
    }

    /**
     * Log in with a JavaFX WebView (Microsoft login page)
     * @return A CompletableFuture that will complete with an AuthProfile
     */
    public CompletableFuture<AuthProfile> loginWithJavaFXWebView()
    {
        return login(new WebViewFrame());
    }

    /**
     * Log in with a JavaFX WebView (Microsoft login page)
     * @param windowWidth width of the WebView
     * @param windowHeight height of the WebView
     * @return A CompletableFuture that will complete with an AuthProfile
     */
    public CompletableFuture<AuthProfile> loginWithJavaFXWebView(int windowWidth, int windowHeight)
    {
        return login(new WebViewFrame(windowWidth, windowHeight));
    }

    private Map.Entry<String, String> pollForDeviceToken(String deviceCode, int interval) throws Exception
    {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", CLIENT_ID);
        params.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");
        params.put("device_code", deviceCode);

        while (true)
        {
            Thread.sleep(interval * 1000L);
            try
            {
                JsonObject response = postForm(DEVICE_TOKEN_URL, params);
                if (response.has("access_token"))
                {
                    return Map.entry(response.get("access_token").getAsString(), response.get("refresh_token").getAsString());
                }
            }
            catch (RuntimeException e)
            {
                if (!e.getMessage().contains("authorization_pending")) throw new AuthenticationException("Device code polling failed or expired", e);
            }
        }
    }

    /**
     * Log in with a refresh token. Safe to call concurrently with the same refresh token from
     * multiple threads.
     * @param refreshToken The refresh token to use
     * @return A CompletableFuture that will complete with an AuthProfile
     */
    public CompletableFuture<AuthProfile> loginWithRefreshToken(String refreshToken)
    {
        return refreshesInFlight.computeIfAbsent(refreshToken, token ->
                CompletableFuture.supplyAsync(() ->
                {
                    try
                    {
                        Map<String, String> params = new HashMap<>();
                        params.put("client_id", CLIENT_ID);
                        params.put("refresh_token", token);
                        params.put("grant_type", "refresh_token");
                        params.put("redirect_uri", REDIRECT_URL);

                        JsonObject response = postForm(TOKEN_URL, params);
                        String newMsToken = response.get("access_token").getAsString();
                        String newRefreshToken = response.get("refresh_token").getAsString();

                        return authenticateMinecraftWithTokens(newMsToken, newRefreshToken);
                    }
                    catch (Exception e)
                    {
                        throw new TokenRefreshException("Failed to refresh Microsoft token", e);
                    }
                }).whenComplete((result, error) -> refreshesInFlight.remove(token)));
    }

    /**
     * Returns the given profile if its access token is still valid, otherwise refreshing it
     * @param profile The profile to check
     * @return A CompletableFuture completing with a valid AuthProfile.
     */
    public CompletableFuture<AuthProfile> refreshIfNeeded(AuthProfile profile)
    {
        if (!profile.isExpired()) return CompletableFuture.completedFuture(profile);

        return loginWithRefreshToken(profile.refreshToken());
    }

    private String extractAuthCode(String redirectUrl)
    {
        if (redirectUrl == null) throw new UserCancelledException("Authentication was cancelled by the user.");

        if (!redirectUrl.contains("code=")) throw new AuthenticationException("Failed to extract auth code from URL.");

        String encodedCode = redirectUrl.split("code=")[1].split("&")[0];
        return URLDecoder.decode(encodedCode, StandardCharsets.UTF_8);
    }

    private Map.Entry<String, String> exchangeCodeForMsTokens(String code)
    {
        try
        {
            Map<String, String> params = new HashMap<>();
            params.put("client_id", CLIENT_ID);
            params.put("code", code);
            params.put("grant_type", "authorization_code");
            params.put("redirect_uri", REDIRECT_URL);
            params.put("scope", SCOPE);

            JsonObject response = postForm(TOKEN_URL, params);
            String msAccessToken = response.get("access_token").getAsString();
            String refreshToken = response.get("refresh_token").getAsString();

            return Map.entry(msAccessToken, refreshToken);
        }
        catch (Exception e)
        {
            throw new AuthenticationException("Failed to exchange auth code for tokens", e);
        }
    }

    private AuthProfile authenticateMinecraft(Map.Entry<String, String> msTokens)
    {
        return authenticateMinecraftWithTokens(msTokens.getKey(), msTokens.getValue());
    }

    private AuthProfile authenticateMinecraftWithTokens(String msAccessToken, String msRefreshToken)
    {
        try
        {
            JsonObject xblPayload = new JsonObject();
            JsonObject xblProps = new JsonObject();
            xblProps.addProperty("AuthMethod", "RPS");
            xblProps.addProperty("SiteName", "user.auth.xboxlive.com");
            xblProps.addProperty("RpsTicket", "d=" + msAccessToken);
            xblPayload.add("Properties", xblProps);
            xblPayload.addProperty("RelyingParty", "http://auth.xboxlive.com");
            xblPayload.addProperty("TokenType", "JWT");

            JsonObject xbl = requestJson("POST", "https://user.auth.xboxlive.com/user/authenticate", xblPayload, null);
            String xblToken = xbl.get("Token").getAsString();
            String userHash = xbl.getAsJsonObject("DisplayClaims").getAsJsonArray("xui").get(0).getAsJsonObject().get("uhs").getAsString();

            JsonObject xstsPayload = new JsonObject();
            JsonObject xstsProps = new JsonObject();
            xstsProps.addProperty("SandboxId", "RETAIL");
            JsonArray tokens = new JsonArray();
            tokens.add(xblToken);
            xstsProps.add("UserTokens", tokens);
            xstsPayload.add("Properties", xstsProps);
            xstsPayload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
            xstsPayload.addProperty("TokenType", "JWT");

            JsonObject xsts = requestJson("POST", "https://xsts.auth.xboxlive.com/xsts/authorize", xstsPayload, null);
            String xstsToken = xsts.get("Token").getAsString();

            JsonObject mcPayload = new JsonObject();
            mcPayload.addProperty("identityToken", "XBL3.0 x=" + userHash + ";" + xstsToken);

            JsonObject mcAuth = requestJson("POST", "https://api.minecraftservices.com/authentication/login_with_xbox", mcPayload, null);
            String mcToken = mcAuth.get("access_token").getAsString();
            long expiresInSeconds = mcAuth.has("expires_in") ? mcAuth.get("expires_in").getAsLong() : DEFAULT_TOKEN_LIFETIME_SECONDS;
            Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);

            JsonObject profile = requestJson("GET", "https://api.minecraftservices.com/minecraft/profile", null, mcToken);

            return new AuthProfile(profile.get("name").getAsString(), profile.get("id").getAsString(), mcToken, msRefreshToken, expiresAt);
        }
        catch (Exception e)
        {
            throw new AuthenticationException("Failed to complete Minecraft authentication", e);
        }
    }

    private JsonObject postForm(String urlStr, Map<String, String> params) throws Exception
    {
        StringBuilder body = new StringBuilder();
        params.forEach((key, value) ->
        {
            if (!body.isEmpty()) body.append("&");
            body.append(encode(key)).append("=").append(encode(value));
        });

        HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream())
        {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        return handleResponse(conn, urlStr);
    }

    private JsonObject requestJson(String method, String urlStr, JsonObject payload, String bearerToken) throws Exception
    {
        HttpURLConnection conn = (HttpURLConnection) new URI(urlStr).toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");

        if (bearerToken != null) conn.setRequestProperty("Authorization", "Bearer " + bearerToken);

        if (payload != null)
        {
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream())
            {
                os.write(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
            }
        }

        return handleResponse(conn, urlStr);
    }

    private JsonObject handleResponse(HttpURLConnection conn, String urlStr) throws Exception
    {
        if (conn.getResponseCode() >= 400)
        {
            InputStream errStream = conn.getErrorStream();
            if (errStream != null)
            {
                try (Scanner scanner = new Scanner(errStream, StandardCharsets.UTF_8).useDelimiter("\\A"))
                {
                    String errorText = scanner.hasNext() ? scanner.next() : "";
                    throw new RuntimeException(errorText);
                }
            }
            throw new RuntimeException("HTTP " + conn.getResponseCode() + " at " + urlStr);
        }

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))
        {
            return gson.fromJson(reader, JsonObject.class);
        }
    }

    private String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}