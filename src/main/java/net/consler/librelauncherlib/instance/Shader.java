package net.consler.librelauncherlib.instance;

import java.io.File;

public class Shader
{
    private final File shaderFile;
    public Shader(File shaderFile)
    {
        this.shaderFile = shaderFile;
    }

    public File getFile()
    {
        return shaderFile;
    }
}
