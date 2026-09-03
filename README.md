LibreLauncherLib
=================

A small Java library for downloading and launching Minecraft versions programmatically.

## API
- net.consler.librelauncherlib.LibreLauncherLib: install(String version, Path rootDirectory) and launch(LaunchProfile profile)
- net.consler.librelauncherlib.download.MinecraftInstaller: install(String version, Path rootDirectory)
- net.consler.librelauncherlib.launch.MinecraftLauncher: launch(LaunchProfile)
- net.consler.librelauncherlib.launch.LaunchProfile: record + Builder


## Usage example
1) Install a version:
    ``` 
    LibreLauncherLib.install("1.20.2", Path.of("/path/to/game"));
    ```

2) Launch using profile:
   ``` 
   LaunchProfile profile = new LaunchProfile.Builder("Player", "1.20.2", Path.of("/path/to/game")).build();
   Process gameProcess = LibreLauncherLib.launch(profile);
   ```
