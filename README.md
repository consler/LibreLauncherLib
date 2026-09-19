LibreLauncherLib
=================

A Java library for downloading and launching Minecraft versions.

## Usage example
1) Install a version:
    ``` java
    new MinecraftInstaller().install(version, gameDir, ModloaderProfile.VANILLA());
    ```

2) Launch:
   ``` java
   LaunchProfile launchProfile = new LaunchProfile.Builder(version, gameDir).build();
   AuthProfile authProfile = new MicrosoftAuthenticator().login(new WebViewFrame()).join();
   ModLoaderProfile modLoaderProfile = new ModLoaderProfile(ModLoaderProfile.FABRIC_ID, "0.19.5")
   
   new MinecraftLauncher().launch(launchProfile, authProfile, modloaderProfile);
   ```
3) List versions:
   ``` java
   System.out.println(Quilt.getVersions())
   System.out.println(Forge.getVersionsCompatibleWith("26.2"));
   ```
