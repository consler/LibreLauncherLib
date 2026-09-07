LibreLauncherLib
=================

A Java library for downloading and launching Minecraft versions.

## Usage example
1) Install a version:
    ``` java
    MinecraftInstaller.install("26.2", Path.of("/path/to/game"), new ModLoaderProfile("fabric", "0.19.5"));
    ```

2) Launch:
   ``` java
   LaunchProfile launchProfile = new LaunchProfile.Builder(version, gameDir).build();
   AuthProfile authProfile = AuthProfile.Offline("Player");
   ModLoaderProfile loaderProfile = new ModLoaderProfile("fabric", "0.19.5");

   new MinecraftLauncher().launch(launchProfile, authProfile, loaderProfile);
   ```
3) List versions:
   ``` java
   System.out.println(Quilt.getVersions())
   System.out.println(Forge.getVersionsCompatibleWith("26.2"));
   ```
