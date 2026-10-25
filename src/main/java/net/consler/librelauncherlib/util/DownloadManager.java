package net.consler.librelauncherlib.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.consler.librelauncherlib.LibraryException;
import net.consler.librelauncherlib.install.InstallerListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the downloading of files.
 */
public class DownloadManager
{
    private static final int MAX_CONCURRENT_DOWNLOADS = 16;
    private static final int MAX_ATTEMPTS = 3;

    private final HttpClient httpClient;
    private final Semaphore downloadLimiter;
    private InstallerListener listener;

    public DownloadManager()
    {
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
        this.downloadLimiter = new Semaphore(MAX_CONCURRENT_DOWNLOADS);
    }

    public void setListener(InstallerListener listener)
    {
        this.listener = listener;
    }

    public JsonObject fetchJson(String url)
    {
        String secureUrl = enforceHttps(url);

        try
        {
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(secureUrl)).timeout(Duration.ofSeconds(30)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new HttpStatusException(response.statusCode(), "Failed to fetch JSON: " + secureUrl);

            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
        catch (Exception e)
        {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            if (e instanceof HttpStatusException) throw (HttpStatusException) e;
            if (e instanceof LibraryException) throw (LibraryException) e;
            throw new JsonFetchException("Failed to fetch JSON from " + secureUrl, e);
        }
    }

    /**
     * Downloads a file and blocks until it is finished. Files that already exist are skipped.
     *
     * @param url The URL to download the file from
     * @param destination The local path the file will be saved to
     */
    public void downloadFile(String url, Path destination)
    {
        downloadFile(new DownloadTask(url, destination));
    }

    /**
     * Downloads a file and blocks until it is finished. Files that already exist with the expected size are skipped.
     * If the task has a size or SHA-1 hash, the downloaded file is verified against it.
     *
     * @param task The file to download
     */
    public void downloadFile(DownloadTask task)
    {
        downloadFile(task, Map.of());
    }

    /**
     * Downloads a file with additional request headers.
     *
     * @param task The file to download
     * @param headers HTTP headers to send with the request
     */
    public void downloadFile(DownloadTask task, Map<String, String> headers)
    {
        if (headers == null) throw new IllegalArgumentException("headers must not be null");
        String secureUrl = enforceHttps(task.url());
        Path destination = task.destination();
        if (Files.isDirectory(destination))
            throw new DownloadFailedException("Destination is a directory, not a file: " + destination);

        try
        {
            if (isValid(task)) return;

            if (listener != null) listener.onNewFile(destination.getFileName().toString());
            Files.createDirectories(destination.getParent());
        }
        catch (IOException e)
        {
            throw new DownloadFailedException("Failed to prepare destination for " + secureUrl, e);
        }

        Path temp = destination.resolveSibling(destination.getFileName().toString() + ".part");
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(secureUrl))
                .timeout(Duration.ofSeconds(30))
                .GET();
        headers.forEach(requestBuilder::header);
        HttpRequest request = requestBuilder.build();

        for (int attempt = 1; ; attempt++)
        {
            int status = -1;

            try
            {
                HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(temp, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                status = response.statusCode();
                if (status < 200 || status >= 300) throw new HttpStatusException(status, "HTTP Server responded with status code: " + status);

                verify(task, temp);
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                return;
            }
            catch (InterruptedException e)
            {
                deleteQuietly(temp);
                Thread.currentThread().interrupt();
                throw new DownloadFailedException("Download interrupted: " + secureUrl, e);
            }
            catch (Exception e)
            {
                deleteQuietly(temp);
                if (attempt >= MAX_ATTEMPTS || isPermanentFailure(status)) throw new DownloadFailedException("Failed to download " + secureUrl, e);

                pause(300L * attempt);
            }
        }
    }

    /**
     * Downloads multiple files in parallel and blocks until all of them are finished.
     * The first failed download cancels all remaining ones and is rethrown.
     *
     * @param tasks The files to download
     */
    public void downloadBatch(List<DownloadTask> tasks)
    {
        if (tasks == null || tasks.isEmpty()) return;

        Map<Path, DownloadTask> uniqueTasks = new LinkedHashMap<>();
        for (DownloadTask task : tasks) uniqueTasks.putIfAbsent(task.destination(), task);

        int totalTasks = uniqueTasks.size();
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger lastPercentage = new AtomicInteger(-1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor())
        {
            ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<>(executor);
            List<Future<Void>> futures = new ArrayList<>();

            for (DownloadTask task : uniqueTasks.values())
            {
                futures.add(completionService.submit(() ->
                {
                    downloadLimiter.acquire();
                    try
                    {
                        downloadFile(task);
                    }
                    finally
                    {
                        downloadLimiter.release();
                    }

                    int percentage = completedCount.incrementAndGet() * 100 / totalTasks;
                    int previous = lastPercentage.getAndAccumulate(percentage, Math::max);
                    if (listener != null && percentage > previous) listener.onNewPercentage(percentage);

                    return null;
                }));
            }

            for (int i = 0; i < totalTasks; i++)
            {
                try
                {
                    completionService.take().get();
                }
                catch (ExecutionException e)
                {
                    cancelAll(futures);
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    throw new DownloadFailedException("Failed to download files", cause);
                }
                catch (InterruptedException e)
                {
                    cancelAll(futures);
                    Thread.currentThread().interrupt();
                    throw new DownloadFailedException("Download interrupted", e);
                }
            }
        }
    }

    private void cancelAll(List<Future<Void>> futures)
    {
        for (Future<Void> future : futures) future.cancel(true);
    }

    private boolean isValid(DownloadTask task)
    {
        try
        {
            Path destination = task.destination();
            if (!Files.isRegularFile(destination)) return false;

            long size = Files.size(destination);
            return task.size() >= 0 ? size == task.size() : size > 0;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    private void verify(DownloadTask task, Path file) throws IOException
    {
        if (task.size() >= 0 && Files.size(file) != task.size()) throw new IOException("Size mismatch for " + task.url());
        if (task.sha1() != null && !task.sha1().equalsIgnoreCase(sha1Of(file))) throw new IOException("Checksum mismatch for " + task.url());
    }

    private String sha1Of(Path file) throws IOException
    {
        try (InputStream in = Files.newInputStream(file))
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);

            return HexFormat.of().formatHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IOException("SHA-1 is not available", e);
        }
    }

    private boolean isPermanentFailure(int status)
    {
        return status >= 400 && status < 500 && status != 408 && status != 429;
    }

    private void pause(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new DownloadFailedException("Download interrupted", e);
        }
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