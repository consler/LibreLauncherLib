package net.consler.librelauncherlib.util;

import java.nio.file.Path;

/**
 * Represents a task for downloading a file from a URL to a local path.
 */
public record DownloadTask(String url, Path destination) {}