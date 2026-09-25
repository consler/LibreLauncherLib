package net.consler.librelauncherlib.nbt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Represents a Named Binary Tag (NBT) file structure.
 * Provides methods to parse, modify, and save NBT data.
 */
public class NBT
{
    /**
     * The file associated with this NBT structure.
     */
    public File file;

    /**
     * The root compound tag containing all data.
     */
    private final Tag.CompoundTag rootTag;

    /**
     * Creates a new empty NBT structure in memory.
     */
    public NBT()
    {
        this.file = null;
        this.rootTag = new Tag.CompoundTag();
    }

    /**
     * Creates an NBT structure wrapping an existing CompoundTag.
     *
     * @param rootTag The root tag to wrap.
     */
    public NBT(Tag.CompoundTag rootTag)
    {
        this.file = null;
        this.rootTag = rootTag != null ? rootTag : new Tag.CompoundTag();
    }

    /**
     * Loads and parses an NBT structure from a file.
     *
     * @param file The file to read the NBT data from.
     * @throws IOException If an I/O error occurs during reading.
     */
    public NBT(File file) throws IOException
    {
        this.file = file;
        this.rootTag = parseFile();
    }

    /**
     * Opens an input stream for the file, handling GZIP compression if detected.
     *
     * @return A data input stream ready for reading NBT data.
     * @throws IOException If an I/O error occurs.
     */
    private DataInputStream openInputStream() throws IOException
    {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        is.mark(2);
        int magic = is.read() | (is.read() << 8);
        is.reset();

        if (magic == GZIPInputStream.GZIP_MAGIC) is = new GZIPInputStream(is);

        return new DataInputStream(is);
    }

    /**
     * Parses the NBT file from the input stream.
     *
     * @return The parsed root compound tag.
     * @throws IOException If the root tag is invalid or an I/O error occurs.
     */
    private Tag.CompoundTag parseFile() throws IOException
    {
        try (DataInputStream in = openInputStream())
        {
            byte typeId = in.readByte();

            if (typeId != TagType.COMPOUND.getId()) throw new IOException("Expected root CompoundTag, found ID: " + typeId);

            in.readUTF();

            return (Tag.CompoundTag) parseTagPayload(TagType.COMPOUND, in);
        }
    }

    /**
     * Parses the payload of a specific tag type.
     *
     * @param type The tag type to parse.
     * @param in The input stream to read from.
     * @return The parsed tag object.
     * @throws IOException If the tag type is unknown or an I/O error occurs.
     */
    private Tag parseTagPayload(TagType type, DataInputStream in) throws IOException
    {
        switch (type)
        {
            case BYTE: return new Tag.ByteTag(in.readByte());
            case SHORT: return new Tag.ShortTag(in.readShort());
            case INT: return new Tag.IntTag(in.readInt());
            case LONG: return new Tag.LongTag(in.readLong());
            case FLOAT: return new Tag.FloatTag(in.readFloat());
            case DOUBLE: return new Tag.DoubleTag(in.readDouble());
            case BYTE_ARRAY:
            {
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                return new Tag.ByteArrayTag(data);
            }
            case STRING: return new Tag.StringTag(in.readUTF());
            case LIST:
            {
                byte elemTypeId = in.readByte();
                TagType elemType = TagType.valueOf(elemTypeId);
                int count = in.readInt();
                Tag.ListTag list = new Tag.ListTag(elemType);

                for (int i = 0; i < count; i++)
                {
                    list.add(parseTagPayload(elemType, in));
                }
                return list;
            }
            case COMPOUND:
            {
                Tag.CompoundTag compound = new Tag.CompoundTag();
                while (true)
                {
                    byte childTypeId = in.readByte();
                    if (childTypeId == TagType.END.getId()) break;

                    TagType childType = TagType.valueOf(childTypeId);
                    String key = in.readUTF();
                    Tag value = parseTagPayload(childType, in);
                    compound.put(key, value);
                }
                return compound;
            }
            case INT_ARRAY:
            {
                int len = in.readInt();
                int[] data = new int[len];
                for (int i = 0; i < len; i++) data[i] = in.readInt();
                return new Tag.IntArrayTag(data);
            }
            case LONG_ARRAY:
            {
                int len = in.readInt();
                long[] data = new long[len];
                for (int i = 0; i < len; i++) data[i] = in.readLong();
                return new Tag.LongArrayTag(data);
            }
            default: throw new IOException("Unexpected tag type: " + type);
        }
    }

    /**
     * Saves the NBT data back to the loaded file with GZIP compression enabled by default.
     *
     * @throws IOException If an I/O error occurs.
     * @throws IllegalStateException If no target file is specified.
     */
    public void save() throws IOException
    {
        if (file == null)
        {
            throw new IllegalStateException("Cannot save without specifying a file target!");
        }
        saveTo(this.file, true);
    }

    /**
     * Saves the NBT data to a target file.
     *
     * @param targetFile The file to save the data to.
     * @param compressGzip True to compress the output with GZIP, false otherwise.
     * @throws IOException If an I/O error occurs during saving.
     */
    public void saveTo(File targetFile, boolean compressGzip) throws IOException
    {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile));
        if (compressGzip) os = new GZIPOutputStream(os);

        try (DataOutputStream out = new DataOutputStream(os))
        {
            out.writeByte(TagType.COMPOUND.getId());
            out.writeUTF("");
            writeTagPayload(rootTag, out);
        }
    }

    /**
     * Writes the payload of a tag to the output stream.
     *
     * @param tag The tag to write.
     * @param out The output stream to write to.
     * @throws IOException If an unknown tag type is encountered or an I/O error occurs.
     */
    private void writeTagPayload(Tag tag, DataOutputStream out) throws IOException
    {
        switch (tag.getType())
        {
            case BYTE -> out.writeByte(((Tag.ByteTag) tag).getValue());
            case SHORT -> out.writeShort(((Tag.ShortTag) tag).getValue());
            case INT -> out.writeInt(((Tag.IntTag) tag).getValue());
            case LONG -> out.writeLong(((Tag.LongTag) tag).getValue());
            case FLOAT -> out.writeFloat(((Tag.FloatTag) tag).getValue());
            case DOUBLE -> out.writeDouble(((Tag.DoubleTag) tag).getValue());
            case BYTE_ARRAY ->
            {
                byte[] bytes = ((Tag.ByteArrayTag) tag).getValue();
                out.writeInt(bytes.length);
                out.write(bytes);
            }
            case STRING -> out.writeUTF(((Tag.StringTag) tag).getValue());
            case LIST ->
            {
                Tag.ListTag list = (Tag.ListTag) tag;
                out.writeByte(list.getElementType().getId());
                out.writeInt(list.getValue().size());
                for (Tag child : list.getValue())
                {
                    writeTagPayload(child, out);
                }
            }
            case COMPOUND ->
            {
                Tag.CompoundTag compound = (Tag.CompoundTag) tag;
                for (Map.Entry<String, Tag> entry : compound.getValue().entrySet())
                {
                    out.writeByte(entry.getValue().getType().getId());
                    out.writeUTF(entry.getKey());
                    writeTagPayload(entry.getValue(), out);
                }
                out.writeByte(TagType.END.getId());
            }
            case INT_ARRAY ->
            {
                int[] ints = ((Tag.IntArrayTag) tag).getValue();
                out.writeInt(ints.length);
                for (int i : ints)
                {
                    out.writeInt(i);
                }
            }
            case LONG_ARRAY ->
            {
                long[] longs = ((Tag.LongArrayTag) tag).getValue();
                out.writeInt(longs.length);
                for (long l : longs)
                {
                    out.writeLong(l);
                }
            }
            default -> throw new IOException("Cannot write unknown tag type: " + tag.getType());
        }

    }

    /**
     * Gets the root compound tag of this NBT file.
     *
     * @return The root compound tag.
     */
    public Tag.CompoundTag getRootTag()
    {
        return rootTag;
    }

    /**
     * Inserts a tag into the root compound.
     *
     * @param key The key to store the tag under.
     * @param tag The tag object to insert.
     */
    public void put(String key, Tag tag)
    {
        rootTag.put(key, tag);
    }

    /**
     * Gets a generic tag from the root compound.
     *
     * @param key The key of the tag.
     * @return The retrieved tag, or null if not found.
     */
    public Tag get(String key)
    {
        return rootTag.get(key);
    }

    /**
     * Checks if the root compound contains a specific key.
     *
     * @param key The key to check.
     * @return True if the key exists, false otherwise.
     */
    public boolean containsKey(String key)
    {
        return rootTag.containsKey(key);
    }

    /**
     * Removes a tag from the root compound.
     *
     * @param key The key of the tag to remove.
     * @return The removed tag, or null if it did not exist.
     */
    public Tag remove(String key)
    {
        return rootTag.remove(key);
    }

    /**
     * Retrieves a byte value.
     *
     * @param key The key of the tag.
     * @return The byte value, or 0 if not found.
     */
    public byte getByte(String key)
    {
        return rootTag.getByte(key);
    }

    /**
     * Retrieves a short value.
     *
     * @param key The key of the tag.
     * @return The short value, or 0 if not found.
     */
    public short getShort(String key)
    {
        return rootTag.getShort(key);
    }

    /**
     * Retrieves an int value.
     *
     * @param key The key of the tag.
     * @return The int value, or 0 if not found.
     */
    public int getInt(String key)
    {
        return rootTag.getInt(key);
    }

    /**
     * Retrieves a long value.
     *
     * @param key The key of the tag.
     * @return The long value, or 0 if not found.
     */
    public long getLong(String key)
    {
        return rootTag.getLong(key);
    }

    /**
     * Retrieves a float value.
     *
     * @param key The key of the tag.
     * @return The float value, or 0.0f if not found.
     */
    public float getFloat(String key)
    {
        return rootTag.getFloat(key);
    }

    /**
     * Retrieves a double value.
     *
     * @param key The key of the tag.
     * @return The double value, or 0.0d if not found.
     */
    public double getDouble(String key)
    {
        return rootTag.getDouble(key);
    }

    /**
     * Retrieves a boolean value from a byte tag.
     *
     * @param key The key of the tag.
     * @return True if the value is non-zero, false otherwise.
     */
    public boolean getBoolean(String key)
    {
        return rootTag.getBoolean(key);
    }

    /**
     * Retrieves a string value.
     *
     * @param key The key of the tag.
     * @return The string value, or an empty string if not found.
     */
    public String getString(String key)
    {
        return rootTag.getString(key);
    }

    /**
     * Retrieves a byte array.
     *
     * @param key The key of the tag.
     * @return The byte array, or an empty array if not found.
     */
    public byte[] getByteArray(String key)
    {
        return rootTag.getByteArray(key);
    }

    /**
     * Retrieves an int array.
     *
     * @param key The key of the tag.
     * @return The int array, or an empty array if not found.
     */
    public int[] getIntArray(String key)
    {
        return rootTag.getIntArray(key);
    }

    /**
     * Retrieves a long array.
     *
     * @param key The key of the tag.
     * @return The long array, or an empty array if not found.
     */
    public long[] getLongArray(String key)
    {
        return rootTag.getLongArray(key);
    }

    /**
     * Retrieves a list tag.
     *
     * @param key The key of the tag.
     * @return The list tag, or an empty end-type list if not found.
     */
    public Tag.ListTag getList(String key)
    {
        return rootTag.getList(key);
    }

    /**
     * Retrieves a compound tag.
     *
     * @param key The key of the tag.
     * @return The compound tag, or an empty compound tag if not found.
     */
    public Tag.CompoundTag getCompound(String key)
    {
        return rootTag.getCompound(key);
    }

    /**
     * Returns a nested compound path safely.
     *
     * @param path A sequential list of keys to traverse.
     * @return The final compound tag found at the end of the path.
     */
    public Tag.CompoundTag getCompoundIn(String... path)
    {
        Tag.CompoundTag current = rootTag;
        for (String key : path)
        {
            current = current.getCompound(key);
        }
        return current;
    }

    /**
     * Directly gets a string from a nested compound path.
     *
     * @param path A sequential list of keys to traverse.
     * @return The retrieved string, or an empty string if not found.
     */
    public String getStringIn(String... path)
    {
        if (path.length == 0) return "";
        if (path.length == 1) return getString(path[0]);

        Tag.CompoundTag target = rootTag;
        for (int i = 0; i < path.length - 1; i++)
        {
            target = target.getCompound(path[i]);
        }
        return target.getString(path[path.length - 1]);
    }

    @Override
    public String toString()
    {
        return rootTag.toFormattedString(0);
    }
}