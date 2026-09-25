package net.consler.librelauncherlib.instance;

import net.consler.librelauncherlib.nbt.NBT;
import net.consler.librelauncherlib.nbt.Tag;
import net.consler.librelauncherlib.nbt.TagType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public record Servers(File file)
{

    /**
     * Data object representing a single server entry in the servers.dat file.
     * Matches the exact NBT structure of Minecraft multiplayer servers.
     */
    public static class ServerEntry
    {
        public String name;
        public String ip;
        public String icon;
        public Boolean acceptTextures;
        public boolean hidden;
        public Integer acceptedCodeOfConduct;

        public ServerEntry(String name, String ip)
        {
            this.name = name;
            this.ip = ip;
        }

        /**
         * Decodes the Base64 icon string into a BufferedImage.
         *
         * @return The parsed BufferedImage, or null if no icon exists or decoding fails.
         */
        public BufferedImage getIconImage()
        {
            if (icon == null || icon.isEmpty())
            {
                return null;
            }

            try
            {
                String cleanBase64 = icon;
                if (cleanBase64.contains(","))
                {
                    cleanBase64 = cleanBase64.split(",")[1];
                }

                byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);
                try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes))
                {
                    return ImageIO.read(bais);
                }
            }
            catch (IOException | IllegalArgumentException e)
            {
                return null;
            }
        }

        /**
         * Converts a BufferedImage into a PNG Base64 string and updates the icon field.
         *
         * @param image The BufferedImage to encode.
         */
        public void setIconImage(BufferedImage image)
        {
            if (image == null)
            {
                this.icon = null;
                return;
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                ImageIO.write(image, "png", baos);
                this.icon = Base64.getEncoder().encodeToString(baos.toByteArray());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to encode server icon to Base64", e);
            }
        }
    }

    /**
     * Reads the servers.dat file and returns a list of all complete server entries.
     */
    public ArrayList<ServerEntry> getServers()
    {
        ArrayList<ServerEntry> list = new ArrayList<>();

        if (!file.exists()) return list;

        try
        {
            NBT serverNbt = new NBT(file);
            Tag.ListTag serversList = serverNbt.getList("servers");

            for (Tag tag : serversList.getValue())
            {
                if (tag instanceof Tag.CompoundTag compound)
                {
                    ServerEntry entry = new ServerEntry(compound.getString("name"), compound.getString("ip"));

                    if (compound.containsKey("icon"))
                    {
                        entry.icon = compound.getString("icon");
                    }
                    if (compound.containsKey("acceptTextures"))
                    {
                        entry.acceptTextures = compound.getByte("acceptTextures") == 1;
                    }
                    if (compound.containsKey("hidden"))
                    {
                        entry.hidden = compound.getByte("hidden") == 1;
                    }
                    if (compound.containsKey("acceptedCodeOfConduct"))
                    {
                        entry.acceptedCodeOfConduct = compound.getInt("acceptedCodeOfConduct");
                    }

                    list.add(entry);
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read servers.dat", e);
        }
        return list;
    }

    /**
     * Replaces the old listServers() method. Returns only the string names
     * of the servers if that is all you need.
     */
    public ArrayList<String> listServerNames()
    {
        ArrayList<String> names = new ArrayList<>();
        for (ServerEntry entry : getServers())
        {
            names.add(entry.name);
        }
        return names;
    }

    /**
     * Saves a list of ServerEntry objects back to the servers.dat file.
     */
    public void saveServers(List<ServerEntry> servers)
    {
        try
        {
            NBT nbt = new NBT();
            Tag.ListTag listTag = new Tag.ListTag(TagType.COMPOUND);

            for (ServerEntry entry : servers)
            {
                Tag.CompoundTag compound = new Tag.CompoundTag();

                compound.put("name", new Tag.StringTag(entry.name != null ? entry.name : "Minecraft Server"));
                compound.put("ip", new Tag.StringTag(entry.ip != null ? entry.ip : "localhost"));

                if (entry.icon != null)
                {
                    compound.put("icon", new Tag.StringTag(entry.icon));
                }
                if (entry.acceptTextures != null)
                {
                    compound.put("acceptTextures", new Tag.ByteTag((byte) (entry.acceptTextures ? 1 : 0)));
                }
                if (entry.hidden)
                {
                    compound.put("hidden", new Tag.ByteTag((byte) 1));
                }
                if (entry.acceptedCodeOfConduct != null)
                {
                    compound.put("acceptedCodeOfConduct", new Tag.IntTag(entry.acceptedCodeOfConduct));
                }

                listTag.add(compound);
            }

            nbt.put("servers", listTag);

            nbt.saveTo(file, false);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to save servers.dat", e);
        }
    }
}