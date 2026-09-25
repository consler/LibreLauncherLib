package net.consler.librelauncherlib.nbt;

public enum TagType
{
    END(0),
    BYTE(1),
    SHORT(2),
    INT(3),
    LONG(4),
    FLOAT(5),
    DOUBLE(6),
    BYTE_ARRAY(7),
    STRING(8),
    LIST(9),
    COMPOUND(10),
    INT_ARRAY(11),
    LONG_ARRAY(12);

    private final byte id;

    TagType(int id)
    {
        this.id = (byte) id;
    }

    public byte getId()
    {
        return id;
    }

    public static TagType valueOf(byte id)
    {
        for (TagType type : values())
        {
            if (type.id == id) return type;
        }
        throw new IllegalArgumentException("Unknown NBT Tag ID: " + id);
    }
}