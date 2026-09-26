package net.consler.librelauncherlib.instance;

import net.consler.librelauncherlib.nbt.NBT;
import net.consler.librelauncherlib.nbt.Tag;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class for handling Minecraft worlds.
 */
public class World
{
    private final NBT rootNbt;

    private final String directoryName;
    private BufferedImage icon = null;

    private boolean allowCommands = false;

    private List<String> enabledDataPacks = new ArrayList<>();
    private List<String> disabledDataPacks = new ArrayList<>();

    private int dataVersion = 0;

    private int difficultyByte = 0;
    private String difficultyString = "";
    private boolean hardcore = false;
    private boolean locked = false;

    private List<String> enabledFeatures = new ArrayList<>();
    private int gameType = 0;
    private boolean initialized = false;
    private long lastPlayed = 0;
    private String levelName = "Unknown World";
    private int[] singleplayerUuid = new int[0];
    private List<String> serverBrands = new ArrayList<>();

    private String spawnDimension = "";
    private int spawnPitch = 0;
    private int spawnYaw = 0;
    private int[] spawnPos = new int[0];

    private long time = 0;
    private int formatVersion = 0;

    private int versionId = 0;
    private String versionName = "Unknown";
    private String versionSeries = "";
    private boolean versionSnapshot = false;

    private List<Integer> versionHistory = new ArrayList<>();
    private boolean wasModded = false;

    /**
     * Represents a local Minecraft world/save.
     * @param worldDir The path to the world directory (must contain level.dat)
     * @throws IOException If the directory does not contain level.dat, or it cannot be read.
     */
    public World(Path worldDir) throws IOException
    {
        directoryName = worldDir.getFileName().toString();

        Path levelDatPath = worldDir.resolve("level.dat");
        if (Files.exists(levelDatPath))
        {
            rootNbt = new NBT(levelDatPath.toFile());
            parseLevelDat(levelDatPath.toFile());
        }
        else
        {
            throw new IOException("level.dat not found in world directory: " + worldDir);
        }

        Path iconPath = worldDir.resolve("icon.png");
        if (Files.exists(iconPath)) loadIcon(iconPath.toFile());
    }

    private void parseLevelDat(File levelDatFile) throws IOException
    {
        try
        {
            NBT rootNbt = new NBT(levelDatFile);
            NBT data = new NBT(rootNbt.getCompound("Data"));

            this.allowCommands = data.getBoolean("allowCommands");

            NBT dataPacksTag = new NBT(data.getCompound("DataPacks"));
            this.enabledDataPacks = extractStringList(dataPacksTag.getList("Enabled"));
            this.disabledDataPacks = extractStringList(dataPacksTag.getList("Disabled"));

            this.dataVersion = data.getInt("DataVersion");

            this.difficultyByte = data.getByte("Difficulty");
            NBT diffSettings = new NBT(data.getCompound("difficulty_settings"));
            this.difficultyString = diffSettings.getString("difficulty");

            this.hardcore = data.getBoolean("hardcore");
            this.locked = data.getBoolean("locked");

            this.enabledFeatures = extractStringList(data.getList("enabled_features"));
            this.gameType = data.getInt("GameType");
            this.initialized = data.getBoolean("initialized");
            this.lastPlayed = data.getLong("LastPlayed");
            this.levelName = data.getString("LevelName");
            this.singleplayerUuid = data.getIntArray("singleplayer_uuid");
            this.serverBrands = extractStringList(data.getList("ServerBrands"));

            NBT spawnTag = new NBT(data.getCompound("spawn"));
            this.spawnDimension = spawnTag.getString("dimension");
            this.spawnPitch = spawnTag.getInt("pitch");
            this.spawnYaw = spawnTag.getInt("yaw");
            int[] posArray = spawnTag.getIntArray("pos");
            this.spawnPos = posArray != null ? posArray : new int[0];

            this.time = data.getLong("Time");
            this.formatVersion = data.getInt("version");

            NBT versionCompound = new NBT(data.getCompound("Version"));
            this.versionId = versionCompound.getInt("Id");
            this.versionName = versionCompound.getString("Name");
            this.versionSeries = versionCompound.getString("Series");
            this.versionSnapshot = versionCompound.getBoolean("Snapshot");

            this.versionHistory = extractIntList(data.getList("version_history"));
            this.wasModded = data.getBoolean("WasModded");
        }
        catch (Exception e)
        {
            throw new IOException("Failed to read level.dat for world: " + directoryName, e);
        }
    }

    private List<String> extractStringList(Tag.ListTag listTag)
    {
        List<String> result = new ArrayList<>();
        if (listTag != null && listTag.getValue() != null)
        {
            for (Tag tag : listTag.getValue())
            {
                if (tag instanceof Tag.StringTag stringTag) result.add(stringTag.getValue());
            }
        }
        return result;
    }

    private List<Integer> extractIntList(Tag.ListTag listTag)
    {
        List<Integer> result = new ArrayList<>();
        if (listTag != null && listTag.getValue() != null)
        {
            for (Tag tag : listTag.getValue())
            {
                if (tag instanceof Tag.IntTag intTag) result.add(intTag.getValue());
            }
        }
        return result;
    }

    private void loadIcon(File iconFile)
    {
        try
        {
            this.icon = ImageIO.read(iconFile);
        }
        catch (IOException e)
        {
            System.err.println("Failed to read world icon for: " + directoryName);
        }
    }

    public NBT getLevelNBT()
    {
        return rootNbt;
    }

    /** @return The directory name of the world folder. */
    public String getDirectoryName()
    {
        return directoryName;
    }

    /** @return The user-facing name of the level. */
    public String getLevelName()
    {
        return levelName;
    }

    /** @return True if cheats/commands are enabled. */
    public boolean allowsCommands()
    {
        return allowCommands;
    }

    /** @return List of enabled data packs. */
    public List<String> getEnabledDataPacks()
    {
        return Collections.unmodifiableList(enabledDataPacks);
    }

    /** @return List of disabled data packs. */
    public List<String> getDisabledDataPacks()
    {
        return Collections.unmodifiableList(disabledDataPacks);
    }

    /** @return The underlying integer data version of the level. */
    public int getDataVersion()
    {
        return dataVersion;
    }

    /** @return Legacy difficulty value: 0 = Peaceful, 1 = Easy, 2 = Normal, 3 = Hard */
    public int getDifficultyByte()
    {
        return difficultyByte;
    }

    /** @return The difficulty of the world as a string (e.g., "normal"). */
    public String getDifficultyString()
    {
        return difficultyString;
    }

    /** @return True if the player respawns in Spectator on death. */
    public boolean isHardcore()
    {
        return hardcore;
    }

    /** @return True if the player cannot change the difficulty of the world. */
    public boolean isDifficultyLocked()
    {
        return locked;
    }

    /** @return A list of experimental features enabled for this world. */
    public List<String> getEnabledFeatures()
    {
        return Collections.unmodifiableList(enabledFeatures);
    }

    /** @return 0 = Survival, 1 = Creative, 2 = Adventure, 3 = Spectator */
    public int getGameType()
    {
        return gameType;
    }

    /** @return True if a world has been initialized properly after creation. */
    public boolean isInitialized()
    {
        return initialized;
    }

    /** @return The Unix time in milliseconds when the level was last loaded. */
    public long getLastPlayed()
    {
        return lastPlayed;
    }

    /** @return The UUID of the player in singleplayer as an integer array. */
    public int[] getSingleplayerUuid()
    {
        return singleplayerUuid;
    }

    /** @return List of brands of the level (e.g., "vanilla"). */
    public List<String> getServerBrands()
    {
        return Collections.unmodifiableList(serverBrands);
    }

    /** @return The spawn dimension (e.g., "minecraft:overworld"). */
    public String getSpawnDimension()
    {
        return spawnDimension;
    }

    /** @return The spawn pitch. */
    public int getSpawnPitch()
    {
        return spawnPitch;
    }

    /** @return The spawn yaw. */
    public int getSpawnYaw()
    {
        return spawnYaw;
    }

    /** @return The spawn position coordinates [X, Y, Z]. */
    public int[] getSpawnPos()
    {
        return spawnPos;
    }

    /** @return The number of ticks since the start of the level. */
    public long getTime()
    {
        return time;
    }

    /** @return File format version of level.dat. */
    public int getFormatVersion()
    {
        return formatVersion;
    }

    /** @return An integer displaying the version data ID. */
    public int getVersionId()
    {
        return versionId;
    }

    /** @return The version name as a string (e.g. "15w32b" or "1.20"). */
    public String getVersionName()
    {
        return versionName;
    }

    /** @return Developing series (e.g., "main" or "ccpreview"). */
    public String getVersionSeries()
    {
        return versionSeries;
    }

    /** @return True if the version is a snapshot, false otherwise. */
    public boolean isVersionSnapshot()
    {
        return versionSnapshot;
    }

    /** @return A list of data versions that this world has used in the past. */
    public List<Integer> getVersionHistory()
    {
        return Collections.unmodifiableList(versionHistory);
    }

    /** @return True if the world was opened in a modified/modded version. */
    public boolean wasModded()
    {
        return wasModded;
    }

    /** @return The loaded icon image, or null if it does not exist. */
    public BufferedImage getIcon()
    {
        return icon;
    }

    /** @return True if this world directory contains a valid icon.png. */
    public boolean hasIcon()
    {
        return icon != null;
    }
}