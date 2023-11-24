package xyz.zcraft.zpixiv.ui.util;

import xyz.zcraft.zpixiv.ui.Main;

import java.net.URL;

public class ResourceLoader {
    public static URL load(String path) {
        return Main.class.getResource(path);
    }
}
