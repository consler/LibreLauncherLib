package net.consler.librelauncherlib.util;

import java.nio.file.Path;

public record DownloadTask(String url, Path destination) {}