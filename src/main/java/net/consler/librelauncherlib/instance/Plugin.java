package net.consler.librelauncherlib.instance;

import java.io.File;

public class Plugin
{
    private final File pluginFile;
    public Plugin(File pluginFile)
    {
        this.pluginFile = pluginFile;
    }

    public File getFile()
    {
        return pluginFile;
    }
}
