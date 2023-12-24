package xyz.zcraft.zpixiv.util;

import xyz.zcraft.zpixiv.api.artwork.Quality;

@lombok.Data
public class Config {
    private int maxCacheSize = 20;
    private String proxyHost = null;
    private String proxyPort = null;
    private Quality imageQuality = Quality.Original;
}
