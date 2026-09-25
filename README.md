<div align="center">

# LibreLauncherLib
### A library for downloading and launching Minecraft.

[![Maven Central](https://img.shields.io/maven-central/v/net.consler/librelauncherlib?label=stable%20release&color=blue)](https://central.sonatype.com/artifact/net.consler/librelauncherlib)
[![Javadoc](https://javadoc.io/badge2/net.consler/librelauncherlib/javadoc.svg)](https://www.javadoc.io/doc/net.consler/librelauncherlib)

[![JitPack](https://img.shields.io/badge/dev%20build-main--SNAPSHOT-blue)](https://jitpack.io/#net.consler/librelauncherlib/main-SNAPSHOT)
[![JitPack Javadoc](https://img.shields.io/badge/javadoc-main--SNAPSHOT-green)](https://javadoc.jitpack.io/net/consler/librelauncherlib/main-SNAPSHOT/javadoc/index.html)

</div>

## Features
* **Downloading any Minecraft version**
* **Launching Minecraft with custom configurations**
* **Support for all major mod loaders**
* **Authenticating with Microsoft**
* **Easy-to-use API**

## Importing

### Maven

Add this to your `pom.xml`:

```xml
<dependencies>
   <dependency>
      <groupId>net.consler</groupId>
      <artifactId>librelauncherlib</artifactId>
      <version>1.2.1</version>
   </dependency>
</dependencies>
```
### Gradle
Add this to your `build.gradle`:
```groovy
dependencies {
    implementation 'net.consler:librelauncherlib:1.2.1'
}
```

## Example usage
1) Install:
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
   
## More information
* **Go to the [wiki](https://github.com/consler/LibreLauncherLib/wiki)**
* **Read the [javadoc](https://www.javadoc.io/doc/net.consler/librelauncherlib)**