package net.consler.librelauncherlib.auth;

import java.util.concurrent.CompletableFuture;

public interface AuthCodeProvider
{
    CompletableFuture<String> getAuthCode(String url);
}