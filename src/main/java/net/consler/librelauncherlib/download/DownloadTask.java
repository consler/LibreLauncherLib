package net.consler.librelauncherlib.download;

import java.nio.file.Path;

record DownloadTask(String url, Path destination) {}