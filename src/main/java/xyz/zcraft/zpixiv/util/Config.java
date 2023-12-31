package xyz.zcraft.zpixiv.util;

import xyz.zcraft.zpixiv.api.artwork.Quality;

import java.net.InetSocketAddress;
import java.net.Proxy;

@lombok.Data
public class Config {
    private int maxCacheSize = 20;
    private String proxyHost = null;
    private int proxyPort = 0;
    private Quality imageQuality = Quality.Original;

    public Proxy parseProxy() {
        if (proxyHost == null || proxyPort == 0) {
            return null;
        } else {
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        }
    }
}
