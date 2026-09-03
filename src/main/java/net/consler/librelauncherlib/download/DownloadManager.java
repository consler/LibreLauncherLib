package net.consler.librelauncherlib.download;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

class DownloadManager
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

        int threads = Math.min(12, Math.max(2, Runtime.getRuntime().availableProcessors() * 2));
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
            if (Files.exists(destination) && Files.size(destination) > 0) return;
            Files.createDirectories(destination.getParent());

            int maxRetries = 3;
            Exception lastException = null;

            for (int attempt = 1; attempt <= maxRetries; attempt++)
            {
                try
                {
                    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                    if (response.statusCode() >= 200 && response.statusCode() < 300)
                    {
                        try (InputStream in = response.body())
                        {
                            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
                        }
                        return;
                    }

                    throw new net.consler.librelauncherlib.exception.HttpStatusException(response.statusCode(), "HTTP Server responded with status code: " + response.statusCode());
                }
                catch (Exception e)
                {
                    lastException = e;
                    if (attempt < maxRetries) Thread.sleep(500L * attempt);
                }
            }

            throw new net.consler.librelauncherlib.exception.DownloadFailedException("Failed to download after " + maxRetries + " attempts: " + url, lastException);
        }
        catch (Exception e)
        {
            if (e instanceof net.consler.librelauncherlib.exception.LibraryException) throw (net.consler.librelauncherlib.exception.LibraryException) e;
            throw new net.consler.librelauncherlib.exception.LibraryException("Failed to download from " + url, e);
        }
    }

    public void downloadBatch(List<DownloadTask> tasks)
    {
        if (tasks.isEmpty()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (DownloadTask task : tasks)
        {
            futures.add(CompletableFuture.runAsync(() ->
            {
                try
                {
                    downloadLimiter.acquire();
                    downloadFile(task.url(), task.destination());
                }
                catch (Exception e)
                {
                    System.err.println("Failed to download: " + task.url());
                }
                finally
                {
                    downloadLimiter.release();
                }
            }, downloadExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void shutdown()
    {
        downloadExecutor.shutdown();
    }
}