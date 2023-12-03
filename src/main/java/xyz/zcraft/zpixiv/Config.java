package xyz.zcraft.zpixiv;

import java.net.Proxy;

public class Config {
    public Proxy proxy;

    private static Config GLOBAL;

    static {
        GLOBAL = new Config();
    }

    public static Config getGlobalConfig() {
        return GLOBAL;
    }
}
