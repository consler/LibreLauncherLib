LibreLauncherLib
=================

A small Java library for downloading and launching Minecraft versions.

## Usage example
1) Install a version:
    ``` java
    LibreLauncherLib.install("26.2", Path.of("/path/to/game"));
    ```

2) Launch using profile:
   ``` java
   LaunchProfile launchProfile = new LaunchProfile.Builder(version, gameDir).build();
   AuthProfile authProfile = AuthProfile.Offline("Player");

   new MinecraftLauncher().launch(launchProfile, authProfile);
   ```
