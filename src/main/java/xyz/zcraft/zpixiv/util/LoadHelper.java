package xyz.zcraft.zpixiv.util;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.GifData;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.ui.Main;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Optional;
import java.util.zip.ZipInputStream;

public class LoadHelper {
    private static final Logger LOG = LogManager.getLogger();

    public static CachedImage loadImage(String urlString, Identifier identifier, int maxTries, @Nullable LoadTask task) {
        Optional<CachedImage> cache = CachedImage.getCache(identifier);
        if (cache.isPresent()) {
            if (task != null) task.setFinished(true);
            return cache.get();
        }
        while (maxTries-- > 0) {
            try {
                URL url = new URL(urlString);
                URLConnection c;

                if (Main.getConfig().parseProxy() != null)
                    c = url.openConnection(Main.getConfig().parseProxy());
                else c = url.openConnection();

                c.setRequestProperty("Referer", "https://www.pixiv.net");

                final CachedImage image = CachedImage.createCache(identifier, path -> {
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

                if (task != null) task.setFinished(true);
                return image;
            } catch (Exception e) {
                LOG.error("Failed to get image " + identifier, e);
            }
        }

        if (task != null) task.setFailed(true);
        throw new RuntimeException("Can't load image.");
    }

    public static CachedImage loadGif(GifData gifData, Identifier identifier, int maxTries, @Nullable LoadTask loadTask, @Nullable LoadTask parseTask) {
        Optional<CachedImage> cache = CachedImage.getCache(identifier);
        if (cache.isPresent()) {
            if (loadTask != null)
                loadTask.setFinished(true);
            if (parseTask != null)
                parseTask.setFinished(true);
            return cache.get();
        }

        while (maxTries-- > 0) {
            try {
                URL url = new URL(gifData.getSrc());
                URLConnection c;

                if (Main.getConfig().parseProxy() != null)
                    c = url.openConnection(Main.getConfig().parseProxy());
                else c = url.openConnection();

                c.setRequestProperty("Referer", PixivClient.Urls.REFERER);

                final InputStream inputStream = c.getInputStream();
                ZipInputStream zis = new ZipInputStream(inputStream);

                AnimatedGifEncoder age = new AnimatedGifEncoder();
                age.setRepeat(0);
                age.setDelay(gifData.getOrigFrame().getJSONObject(0).getInteger("delay"));

                final CachedImage cache1 = CachedImage.createCache(identifier, path -> {
                    try {
                        age.start(Files.newOutputStream(path));
                        final double total = gifData.getOrigFrame().size();
                        double cur = 0;
                        while (zis.getNextEntry() != null) {
                            age.addFrame(ImageIO.read(zis));
                            if (loadTask != null) loadTask.setProgress(++cur / total);
                        }

                        age.finish();

                        if (loadTask != null) loadTask.setFinished(true);
                        zis.close();
                        inputStream.close();
                        return new Image(path.toFile().toURI().toURL().toString());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                if (parseTask != null) parseTask.setFinished(true);

                return cache1;
            } catch (Exception e) {
                LOG.error("Failed to get gif " + identifier, e);
            }
        }

        if (loadTask != null) loadTask.setFailed(true);
        if (parseTask != null) parseTask.setFailed(true);
        throw new RuntimeException("Can't load gif.");
    }
}
