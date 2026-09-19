package net.consler.librelauncherlib.auth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AuthStore
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveProfile(AuthProfile profile, File file) throws IOException
    {
        if (file.getParentFile() != null) file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file))
        {
            GSON.toJson(profile, writer);
        }
    }

    public static AuthProfile loadProfile(File file) throws IOException
    {
        if (!file.exists()) return null;

        try (FileReader reader = new FileReader(file))
        {
            return GSON.fromJson(reader, AuthProfile.class);
        }
    }
}