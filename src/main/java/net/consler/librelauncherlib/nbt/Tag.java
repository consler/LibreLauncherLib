package net.consler.librelauncherlib.nbt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a generic NBT tag.
 */
public abstract class Tag
{
    /**
     * Gets the specific type of this tag.
     *
     * @return The tag type.
     */
    public abstract TagType getType();

    /**
     * Gets the underlying stored value of this tag.
     *
     * @return The tag value.
     */
    public abstract Object getValue();

    /**
     * Converts the tag to a formatted, readable string.
     *
     * @param indentLevel The current indentation level for formatting.
     * @return The formatted string representation.
     */
    public abstract String toFormattedString(int indentLevel);

    @Override
    public String toString()
    {
        return toFormattedString(0);
    }

    /**
     * Creates a string of spaces for indentation.
     *
     * @param level The indentation depth.
     * @return A string containing the required indentation.
     */
    protected String createIndent(int level)
    {
        return "  ".repeat(Math.max(0, level));
    }

    /**
     * Represents a single byte tag.
     */
    public static class ByteTag extends Tag
    {
        private final byte value;

        /**
         * @param value The byte value to store.
         */
        public ByteTag(byte value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.BYTE;
        }
        @Override public Byte getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return value + "b";
        }
    }

    /**
     * Represents a single short tag.
     */
    public static class ShortTag extends Tag
    {
        private final short value;

        /**
         * @param value The short value to store.
         */
        public ShortTag(short value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.SHORT;
        }
        @Override public Short getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return value + "s";
        }
    }

    /**
     * Represents a single int tag.
     */
    public static class IntTag extends Tag
    {
        private final int value;

        /**
         * @param value The int value to store.
         */
        public IntTag(int value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.INT;
        }
        @Override public Integer getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return String.valueOf(value);
        }
    }

    /**
     * Represents a single long tag.
     */
    public static class LongTag extends Tag
    {
        private final long value;

        /**
         * @param value The long value to store.
         */
        public LongTag(long value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.LONG;
        }
        @Override public Long getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return value + "L";
        }
    }

    /**
     * Represents a single float tag.
     */
    public static class FloatTag extends Tag
    {
        private final float value;

        /**
         * @param value The float value to store.
         */
        public FloatTag(float value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.FLOAT;
        }
        @Override public Float getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return value + "f";
        }
    }

    /**
     * Represents a single double tag.
     */
    public static class DoubleTag extends Tag
    {
        private final double value;

        /**
         * @param value The double value to store.
         */
        public DoubleTag(double value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.DOUBLE;
        }
        @Override public Double getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return value + "d";
        }
    }

    /**
     * Represents a tag containing a primitive byte array.
     */
    public static class ByteArrayTag extends Tag
    {
        private final byte[] value;

        /**
         * @param value The byte array to store.
         */
        public ByteArrayTag(byte[] value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.BYTE_ARRAY;
        }
        @Override public byte[] getValue()
        {
            return value;
        }

        @Override
        public String toFormattedString(int indent)
        {
            StringBuilder sb = new StringBuilder("[B;");
            for (int i = 0; i < value.length; i++)
            {
                sb.append(value[i]).append("b");
                if (i < value.length - 1) sb.append(",");
            }
            return sb.append("]").toString();
        }
    }

    /**
     * Represents a tag containing a UTF-8 string.
     */
    public static class StringTag extends Tag
    {
        private final String value;

        /**
         * @param value The string value to store.
         */
        public StringTag(String value)
        {
            this.value = value;
        }
        @Override public TagType getType()
        {
            return TagType.STRING;
        }
        @Override public String getValue()
        {
            return value;
        }
        @Override public String toFormattedString(int indent)
        {
            return "\"" + value + "\"";
        }
    }

    /**
     * Represents a list of unnamed tags of the same type.
     */
    public static class ListTag extends Tag
    {
        private final TagType elementType;
        private final List<Tag> value = new ArrayList<>();

        /**
         * @param elementType The type of tags this list will hold.
         */
        public ListTag(TagType elementType)
        {
            this.elementType = elementType;
        }

        /**
         * Gets the allowed tag type for elements in this list.
         *
         * @return The element tag type.
         */
        public TagType getElementType()
        {
            return elementType;
        }

        /**
         * Adds a tag to the list.
         *
         * @param tag The tag to add.
         */
        public void add(Tag tag)
        {
            this.value.add(tag);
        }

        /**
         * Gets the number of elements in the list.
         *
         * @return The list size.
         */
        public int size()
        {
            return this.value.size();
        }

        /**
         * Gets the tag at the specified index.
         *
         * @param index The position in the list.
         * @return The tag at the index.
         */
        public Tag get(int index)
        {
            return this.value.get(index);
        }

        @Override public TagType getType()
        {
            return TagType.LIST;
        }
        @Override public List<Tag> getValue()
        {
            return value;
        }

        @Override
        public String toFormattedString(int indent)
        {
            if (value.isEmpty()) return "[]";

            StringBuilder sb = new StringBuilder("[\n");
            String childIndent = createIndent(indent + 1);

            for (int i = 0; i < value.size(); i++)
            {
                sb.append(childIndent).append(value.get(i).toFormattedString(indent + 1));
                if (i < value.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append(createIndent(indent)).append("]");
            return sb.toString();
        }
    }

    /**
     * Represents a collection of named tags.
     */
    public static class CompoundTag extends Tag
    {
        private final Map<String, Tag> value = new HashMap<>();

        /**
         * Inserts a tag into the compound.
         *
         * @param key The key to store the tag under.
         * @param tag The tag object to insert.
         */
        public void put(String key, Tag tag) { this.value.put(key, tag); }

        /**
         * Retrieves a generic tag by key.
         *
         * @param key The key to look up.
         * @return The tag, or null if not found.
         */
        public Tag get(String key)
        {
            return this.value.get(key);
        }

        /**
         * Checks if a key exists in the compound.
         *
         * @param key The key to check.
         * @return True if the key exists, false otherwise.
         */
        public boolean containsKey(String key)
        {
            return this.value.containsKey(key);
        }

        /**
         * Removes a tag from the compound.
         *
         * @param key The key of the tag to remove.
         * @return The removed tag, or null if it did not exist.
         */
        public Tag remove(String key)
        {
            return this.value.remove(key);
        }

        /**
         * Gets all keys present in the compound.
         *
         * @return A set of keys.
         */
        public Set<String> keySet()
        {
            return this.value.keySet();
        }

        /**
         * Gets the total number of tags in the compound.
         *
         * @return The tag count.
         */
        public int size()
        {
            return this.value.size();
        }

        /**
         * Checks if the compound is empty.
         *
         * @return True if no tags are stored, false otherwise.
         */
        public boolean isEmpty()
        {
            return this.value.isEmpty();
        }

        /**
         * Retrieves a byte value.
         *
         * @param key The key of the tag.
         * @return The byte value, or 0 if not found.
         */
        public byte getByte(String key)
        {
            Tag tag = get(key);
            return (tag instanceof ByteTag) ? ((ByteTag) tag).getValue() : 0;
        }

        /**
         * Retrieves a short value.
         *
         * @param key The key of the tag.
         * @return The short value, or 0 if not found.
         */
        public short getShort(String key)
        {
            Tag tag = get(key);
            return (tag instanceof ShortTag) ? ((ShortTag) tag).getValue() : 0;
        }

        /**
         * Retrieves an int value.
         *
         * @param key The key of the tag.
         * @return The int value, or 0 if not found.
         */
        public int getInt(String key)
        {
            Tag tag = get(key);
            return (tag instanceof IntTag) ? ((IntTag) tag).getValue() : 0;
        }

        /**
         * Retrieves a long value.
         *
         * @param key The key of the tag.
         * @return The long value, or 0L if not found.
         */
        public long getLong(String key)
        {
            Tag tag = get(key);
            return (tag instanceof LongTag) ? ((LongTag) tag).getValue() : 0L;
        }

        /**
         * Retrieves a float value.
         *
         * @param key The key of the tag.
         * @return The float value, or 0.0f if not found.
         */
        public float getFloat(String key)
        {
            Tag tag = get(key);
            return (tag instanceof FloatTag) ? ((FloatTag) tag).getValue() : 0.0f;
        }

        /**
         * Retrieves a double value.
         *
         * @param key The key of the tag.
         * @return The double value, or 0.0d if not found.
         */
        public double getDouble(String key)
        {
            Tag tag = get(key);
            return (tag instanceof DoubleTag) ? ((DoubleTag) tag).getValue() : 0.0d;
        }

        /**
         * Retrieves a boolean value from a byte tag.
         *
         * @param key The key of the tag.
         * @return True if the byte value is non-zero, false otherwise.
         */
        public boolean getBoolean(String key)
        {
            Tag tag = get(key);
            if (tag instanceof ByteTag) return ((ByteTag) tag).getValue() != 0;
            return false;
        }

        /**
         * Retrieves a string value.
         *
         * @param key The key of the tag.
         * @return The string value, or an empty string if not found.
         */
        public String getString(String key)
        {
            Tag tag = get(key);
            return (tag instanceof StringTag) ? ((StringTag) tag).getValue() : "";
        }

        /**
         * Retrieves a byte array.
         *
         * @param key The key of the tag.
         * @return The byte array, or an empty array if not found.
         */
        public byte[] getByteArray(String key)
        {
            Tag tag = get(key);
            return (tag instanceof ByteArrayTag) ? ((ByteArrayTag) tag).getValue() : new byte[0];
        }

        /**
         * Retrieves an int array.
         *
         * @param key The key of the tag.
         * @return The int array, or an empty array if not found.
         */
        public int[] getIntArray(String key)
        {
            Tag tag = get(key);
            return (tag instanceof IntArrayTag) ? ((IntArrayTag) tag).getValue() : new int[0];
        }

        /**
         * Retrieves a long array.
         *
         * @param key The key of the tag.
         * @return The long array, or an empty array if not found.
         */
        public long[] getLongArray(String key)
        {
            Tag tag = get(key);
            return (tag instanceof LongArrayTag) ? ((LongArrayTag) tag).getValue() : new long[0];
        }

        /**
         * Retrieves a list tag.
         *
         * @param key The key of the tag.
         * @return The list tag, or an empty end-type list if not found.
         */
        public ListTag getList(String key)
        {
            Tag tag = get(key);
            return (tag instanceof ListTag) ? (ListTag) tag : new ListTag(TagType.END);
        }

        /**
         * Retrieves a compound tag.
         *
         * @param key The key of the tag.
         * @return The compound tag, or an empty compound tag if not found.
         */
        public CompoundTag getCompound(String key)
        {
            Tag tag = get(key);
            return (tag instanceof CompoundTag) ? (CompoundTag) tag : new CompoundTag();
        }

        @Override public TagType getType()
        {
            return TagType.COMPOUND;
        }
        @Override public Map<String, Tag> getValue()
        {
            return value;
        }

        @Override
        public String toFormattedString(int indent)
        {
            if (value.isEmpty()) return "{}";

            StringBuilder sb = new StringBuilder("{\n");
            String childIndent = createIndent(indent + 1);
            int count = 0;

            for (Map.Entry<String, Tag> entry : value.entrySet())
            {
                sb.append(childIndent).append("\"").append(entry.getKey()).append("\": ").append(entry.getValue().toFormattedString(indent + 1));

                if (++count < value.size()) sb.append(",");
                sb.append("\n");
            }

            sb.append(createIndent(indent)).append("}");
            return sb.toString();
        }
    }

    /**
     * Represents a tag containing a primitive int array.
     */
    public static class IntArrayTag extends Tag
    {
        private final int[] value;

        /**
         * @param value The int array to store.
         */
        public IntArrayTag(int[] value) { this.value = value; }
        @Override public TagType getType() { return TagType.INT_ARRAY; }
        @Override public int[] getValue() { return value; }

        @Override
        public String toFormattedString(int indent)
        {
            StringBuilder sb = new StringBuilder("[I;");
            for (int i = 0; i < value.length; i++)
            {
                sb.append(value[i]);
                if (i < value.length - 1) sb.append(",");
            }
            return sb.append("]").toString();
        }
    }

    /**
     * Represents a tag containing a primitive long array.
     */
    public static class LongArrayTag extends Tag
    {
        private final long[] value;

        /**
         * @param value The long array to store.
         */
        public LongArrayTag(long[] value) { this.value = value; }
        @Override public TagType getType() { return TagType.LONG_ARRAY; }
        @Override public long[] getValue() { return value; }

        @Override
        public String toFormattedString(int indent)
        {
            StringBuilder sb = new StringBuilder("[L;");
            for (int i = 0; i < value.length; i++)
            {
                sb.append(value[i]).append("L");
                if (i < value.length - 1) sb.append(",");
            }
            return sb.append("]").toString();
        }
    }
}