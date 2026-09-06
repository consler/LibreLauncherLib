package net.consler.librelauncherlib.install;

import java.nio.file.Path;

record DownloadTask(String url, Path destination) {}