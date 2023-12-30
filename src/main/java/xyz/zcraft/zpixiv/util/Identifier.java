package xyz.zcraft.zpixiv.util;

import lombok.Data;
import xyz.zcraft.zpixiv.api.artwork.Quality;

@Data
public class Identifier {
    private final String id;
    private final Type type;
    private final int index;
    private final String quality;

    private Identifier(String id, Type type, int index, String quality) {
        this.id = id;
        this.type = type;
        this.index = index;
        this.quality = quality;
    }

    @SuppressWarnings("unused")
    public enum Type {
        Artwork, Gif, Author
    }

    public static Identifier of(String id, Type type, int index, Quality quality) {
        return new Identifier(id, type, index, quality.name());
    }
}


