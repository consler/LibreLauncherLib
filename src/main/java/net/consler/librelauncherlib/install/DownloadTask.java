package net.consler.librelauncherlib.install;

import java.nio.file.Path;

public record DownloadTask(String url, Path destination) {}