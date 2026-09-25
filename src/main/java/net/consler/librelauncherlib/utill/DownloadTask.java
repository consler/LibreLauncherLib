package net.consler.librelauncherlib.utill;

import java.nio.file.Path;

public record DownloadTask(String url, Path destination) {}