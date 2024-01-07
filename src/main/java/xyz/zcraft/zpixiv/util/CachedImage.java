package xyz.zcraft.zpixiv.util;

import javafx.scene.image.Image;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.ui.Main;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;

@Getter
@EqualsAndHashCode(of = {"identifier"})
public class CachedImage {
    private final Path path;
    private final Identifier identifier;
    private final Image image;
    private boolean unused = false;

    private CachedImage(Path path, Identifier id, Image image) {
        this.path = path;
        this.identifier = id;
        this.image = image;
        path.toFile().deleteOnExit();
    }

    public static Optional<CachedImage> getCache(Identifier identifier) {
        return cachePool.stream().filter(e -> e.identifier.equals(identifier)).findAny();
    }

    public static CachedImage createCache(Identifier identifier, Function<Path, Image> create) throws Exception {
        Optional<CachedImage> ca = getCache(identifier);
        if (ca.isPresent()) return ca.get();

        Path tempFile = Files.createTempFile("zpixiv-", ".tmp");
        tempFile.toFile().deleteOnExit();
        return new CachedImage(tempFile, identifier, create.apply(tempFile));
    }

    private final static LinkedList<CachedImage> cachePool = new LinkedList<>();

    public void addToCache() {
        tidyCache();
        if (!cachePool.contains(this)) {
            cachePool.add(this);
        }
    }

    private static void tidyCache() {
        if (cachePool.size() + 1 > Main.getConfig().getMaxCacheSize()) {
            for (int i = 0; i < cachePool.size() - Main.getConfig().getMaxCacheSize(); i++) {
                Optional<CachedImage> first = cachePool.stream().filter(e -> e.unused).findFirst();
                if (first.isPresent()) {
                    cachePool.remove(first.get());
                } else {
                    return;
                }
            }
        }
    }

    public void markUnused() {
        unused = true;
    }
}
