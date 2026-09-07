package net.consler.librelauncherlib.utill;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.exception.DownloadFailedException;
import net.consler.librelauncherlib.exception.HttpStatusException;
import net.consler.librelauncherlib.install.DownloadTask;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

public class DownloadManager
{

    private final HttpClient httpClient;
    private final ExecutorService downloadExecutor;
    private final Semaphore downloadLimiter;

    public DownloadManager()
    {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        int threads = Math.clamp(Runtime.getRuntime().availableProcessors() * 2L, 2, 12);
        this.downloadExecutor = Executors.newFixedThreadPool(threads);
        this.downloadLimiter = new Semaphore(Math.max(4, threads / 2));
    }

    public JsonObject fetchJson(String url)
    {
        try
        {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300)
            {
                throw new net.consler.librelauncherlib.exception.HttpStatusException(response.statusCode(), "Failed to fetch JSON: " + url);
            }

            return JsonParser.parseReader(new InputStreamReader(response.body())).getAsJsonObject();
        }
        catch (Exception e)
        {
            if (e instanceof net.consler.librelauncherlib.exception.LibraryException) throw (net.consler.librelauncherlib.exception.LibraryException) e;
            throw new net.consler.librelauncherlib.exception.LibraryException("Failed to fetch JSON from " + url, e);
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
            Throwable cause = e instanceof java.util.concurrent.CompletionException ? e.getCause() : e;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new DownloadFailedException("Failed to install from " + url, cause);
        }
    }

    public CompletableFuture<Void> downloadFileAsync(String url, Path destination)
    {
        System.out.println("Downloading " + url);

        try
        {
            if (Files.exists(destination) && Files.size(destination) > 0) return CompletableFuture.completedFuture(null);
            Files.createDirectories(destination.getParent());
        }
        catch (Exception e)
        {
            return CompletableFuture.failedFuture(new DownloadFailedException("Failed to prepare destination for " + url, e));
        }

        final int maxRetries = 3;
        final Path temp = destination.resolveSibling(destination.getFileName().toString() + ".part");

        Function<Integer, CompletableFuture<Void>> attempt = new Function<>()
        {
            @Override
            public CompletableFuture<Void> apply(Integer attemptNum)
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
                                    try { Files.deleteIfExists(response.body()); } catch (Exception ignored) {}
                                    return CompletableFuture.failedFuture(new DownloadFailedException("Failed to move downloaded file for " + url, e));
                                }
                            }
                            else
                            {
                                try
                                {
                                    Files.deleteIfExists(response.body());
                                }
                                catch (Exception ignored)
                                {
                                }
                                if (attemptNum < maxRetries)
                                {
                                    long delay = 300L * attemptNum;
                                    return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS, downloadExecutor))
                                            .thenCompose(ignored -> apply(attemptNum + 1));
                                }
                                return CompletableFuture.failedFuture(new HttpStatusException(status, "HTTP Server responded with status code: " + status));
                            }
                        })
                        .<CompletableFuture<Void>>handle((respOrNull, ex) ->
                        {
                            if (ex == null) return CompletableFuture.completedFuture(null);

                            Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                            try
                            {
                                Files.deleteIfExists(temp);
                            }
                            catch (Exception ignored)
                            {
                            }
                            if (attemptNum < maxRetries)
                            {
                                long delay = 300L * attemptNum;
                                return CompletableFuture.supplyAsync(() -> null, CompletableFuture.delayedExecutor(delay, java.util.concurrent.TimeUnit.MILLISECONDS, downloadExecutor))
                                        .thenCompose(ignored -> apply(attemptNum + 1));
                            }
                            return CompletableFuture.failedFuture(new DownloadFailedException("Failed to download " + url, cause));
                        }).thenCompose(cf -> cf);
            }
        };

        return attempt.apply(1);
    }

    public void downloadBatch(List<DownloadTask> tasks)
    {
        if (tasks.isEmpty()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (DownloadTask task : tasks)
        {
            CompletableFuture<Void> f = CompletableFuture.runAsync(() ->
                {
                        try
                        {
                            downloadLimiter.acquire();
                        }
                        catch (InterruptedException e)
                        {
                            throw new RuntimeException(e);
                        }
                }, downloadExecutor)
                    .thenCompose(ignored -> downloadFileAsync(task.url(), task.destination()))
                    .whenComplete((v, e) -> downloadLimiter.release())
                    .exceptionally(ex ->
                    {
                        throw new DownloadFailedException("Failed to download " + task.url(), ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex);
                    });

            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void shutdown()
    {
        downloadExecutor.shutdown();
    }
}