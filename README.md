LibreLauncherLib
=================

A small Java library for downloading and launching Minecraft versions programmatically.

Artifact coordinates
<dependency>
    <groupId>net.consler</groupId>
    <artifactId>librelauncherlib</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

Public API
- net.consler.librelauncherlib.LibreLauncherLib: static install(...) and launch(...)
- net.consler.librelauncherlib.download.MinecraftInstaller: install(String version, Path rootDirectory)
- net.consler.librelauncherlib.launch.MinecraftLauncher: launch(LaunchProfile)
- net.consler.librelauncherlib.launch.LaunchProfile: record + Builder

Design notes
- Internal classes (download helpers, processors, native extractor, util) are package-private.
- Runtime exceptions are custom and live under net.consler.librelauncherlib.exception.
- Common custom exceptions include VersionJsonMissingException, VersionNotFoundException, DownloadFailedException, HttpStatusException, InstallationException, LaunchException, and NativesExtractionException.

Usage example
1) Install a version:
   LibreLauncherLib.install("1.20.2", Path.of("/path/to/game"));

2) Launch using profile:
   LaunchProfile profile = new LaunchProfile.Builder("Player", "1.20.2", Path.of("/path/to/game")).build();
   Process gameProcess = LibreLauncherLib.launch(profile);

Notes
- This repo is packaged as a Maven library with Javadocs and sources attached.
- Internal implementation details remain package-private to keep the public API clean and easier to reference from other projects.
