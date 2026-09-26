package net.consler.librelauncherlib.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.DownloadFailedException;
import net.consler.librelauncherlib.exception.HttpStatusException;
import net.consler.librelauncherlib.exception.LibraryException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class DownloadManager
{
    private final HttpClient httpClient;
    private final ExecutorService downloadExecutor;
    private final Semaphore downloadLimiter;

    public DownloadManager()
    {
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();

        int threads = Math.clamp(Runtime.getRuntime().availableProcessors() * 2L, 2, 12);
        this.downloadExecutor = Executors.newFixedThreadPool(threads);
        this.downloadLimiter = new Semaphore(Math.max(4, threads / 2));
    }

    public JsonObject fetchJson(String url)
    {
        String secureUrl = enforceHttps(url);

        try
        {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(secureUrl)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new HttpStatusException(response.statusCode(), "Failed to fetch JSON: " + secureUrl);

            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
        catch (Exception e)
        {
            if (e instanceof LibraryException) throw (LibraryException) e;
            throw new LibraryException("Failed to fetch JSON from " + secureUrl, e);
        }
    }

    public void downloadFile(String url, Path destination)
    {
        try
        {
            downloadFileAsync(url, destination).join();
        }
        catch (Exception e)
        {
            Throwable cause = e instanceof CompletionException ? e.getCause() : e;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new DownloadFailedException("Failed to install from " + url, cause);
        }
    }

    public CompletableFuture<Void> downloadFileAsync(String url, Path destination)
    {
        String secureUrl = enforceHttps(url);
        System.out.println("Downloading " + secureUrl);

        try
        {
            if (Files.exists(destination) && Files.size(destination) > 0)
            {
                return CompletableFuture.completedFuture(null);
            }
            Files.createDirectories(destination.getParent());
        }
        catch (Exception e)
        {
            return CompletableFuture.failedFuture(new DownloadFailedException("Failed to prepare destination for " + secureUrl, e));
        }

        Path temp = destination.resolveSibling(destination.getFileName().toString() + ".part");
        return downloadWithRetryAsync(secureUrl, destination, temp, 1, 3);
    }

    private CompletableFuture<Void> downloadWithRetryAsync(String url, Path destination, Path temp, int attempt, int maxRetries)
    {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(30)).GET().build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofFile(temp))
                .thenCompose(response ->
                {
                    int status = response.statusCode();
                    if (status >= 200 && status < 300)
                    {
                        try
                        {
                            Files.move(response.body(), destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                            return CompletableFuture.completedFuture(null);
                        }
                        catch (Exception e)
                        {
                            deleteQuietly(temp);
                            return CompletableFuture.failedFuture(new DownloadFailedException("Failed to move downloaded file for " + url, e));
                        }
                    }
                    else
                    {
                        deleteQuietly(temp);
                        return retryOrFail(url, destination, temp, attempt, maxRetries, new HttpStatusException(status, "HTTP Server responded with status code: " + status));
                    }
                })
                .exceptionallyCompose(ex ->
                {
                    deleteQuietly(temp);
                    return retryOrFail(url, destination, temp, attempt, maxRetries, ex);
                });
    }

    private CompletableFuture<Void> retryOrFail(String url, Path destination, Path temp, int attempt, int maxRetries, Throwable cause)
    {
        if (attempt < maxRetries)
        {
            long delay = 300L * attempt;
            return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS, downloadExecutor)).thenCompose(ignored -> downloadWithRetryAsync(url, destination, temp, attempt + 1, maxRetries));
        }

        Throwable actualCause = cause instanceof CompletionException ? cause.getCause() : cause;
        return CompletableFuture.failedFuture(new DownloadFailedException("Failed to download " + url, actualCause));
    }

    public void downloadBatch(List<DownloadTask> tasks)
    {
        if (tasks == null || tasks.isEmpty()) return;


        List<CompletableFuture<Void>> futures = tasks.stream().map(task ->
                CompletableFuture.runAsync(() ->
                        {
                            try
                            {
                                downloadLimiter.acquire();
                            }
                            catch (InterruptedException e)
                            {
                                Thread.currentThread().interrupt();
                                throw new RuntimeException(e);
                            }
                        }, downloadExecutor)
                        .thenCompose(ignored -> downloadFileAsync(task.url(), task.destination()))
                        .whenComplete((v, e) -> downloadLimiter.release())
                        .exceptionally(ex ->
                        {
                            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                            throw new DownloadFailedException("Failed to download " + task.url(), cause);
                        })
        ).toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void shutdown()
    {
        downloadExecutor.shutdown();
    }

    private String enforceHttps(String url)
    {
        if (url != null && url.startsWith("http://"))
        {
            return url.replaceFirst("http://", "https://");
        }
        return url;
    }

    private void deleteQuietly(Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (Exception ignored)
        {
        }
    }
}