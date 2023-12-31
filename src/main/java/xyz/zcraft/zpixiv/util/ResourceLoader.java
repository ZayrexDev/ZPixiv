package xyz.zcraft.zpixiv.util;

import xyz.zcraft.zpixiv.ui.Main;

import java.io.InputStream;
import java.net.URL;

public class ResourceLoader {
    public static URL load(String path) {
        return Main.class.getResource(path);
    }
    public static InputStream loadAsStream(String path) {
        return Main.class.getResourceAsStream(path);
    }
}
