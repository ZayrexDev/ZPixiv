package xyz.zcraft.zpixiv.util;

import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.ui.Main;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;

public class LoadHelper {
    private static final Logger LOG = LogManager.getLogger();

    public static CachedImage loadImage(String urlString, Identifier identifier, int maxTries, @Nullable LoadTask task) {
        while (maxTries-- > 0) {
            try {
                URL url = new URL(urlString);
                URLConnection c;

                if (Main.getConfig().parseProxy() != null)
                    c = url.openConnection(Main.getConfig().parseProxy());
                else c = url.openConnection();

                c.setRequestProperty("Referer", "https://www.pixiv.net");

                CachedImage image;
                Optional<CachedImage> cache = CachedImage.getCache(identifier);
                if (cache.isPresent()) {
                    image = cache.get();
                } else {
                    image = CachedImage.createCache(identifier, path -> {
                        try {
                            OutputStream o = Files.newOutputStream(path);
                            var b = new BufferedInputStream(c.getInputStream());
                            int contentLength = c.getContentLength();
                            int curLength = 0;
                            byte[] buffer = new byte[10240];
                            int size;
                            while ((size = b.read(buffer)) != -1) {
                                curLength += size;
                                if (task != null) task.setProgress(Math.min((double) curLength / contentLength, 1.0));
                                o.write(buffer, 0, size);
                            }
                            o.close();
                            b.close();
                            return new Image(path.toFile().toURI().toURL().toString());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    image.addToCache();
                }

                if (task != null) task.setFinished(true);
                return image;
            } catch (Exception e) {
                LOG.error("Failed to get image " + identifier, e);
            }
        }

        if (task != null) task.setFailed(true);
        throw new RuntimeException("Can't load image.");
    }
}
