package net.consler.librelauncherlib.util;

import com.google.gson.JsonObject;

import java.nio.file.Path;

/**
 * Represents a task for downloading a file from a URL to a local path.
 */
public record DownloadTask(String url, Path destination, String sha1, long size)
{
    public DownloadTask(String url, Path destination)
    {
        this(url, destination, null, -1);
    }

    public static DownloadTask of(JsonObject download, Path destination)
    {
        String sha1 = download.has("sha1") ? download.get("sha1").getAsString() : null;
        long size = download.has("size") ? download.get("size").getAsLong() : -1;
        return new DownloadTask(download.get("url").getAsString(), destination, sha1, size);
    }
}