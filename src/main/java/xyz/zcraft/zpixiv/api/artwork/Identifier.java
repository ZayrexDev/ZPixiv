package xyz.zcraft.zpixiv.api.artwork;

import lombok.Data;

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
        Artwork, Gif, Profile
    }

    public static Identifier of(String id, Type type, int index, Quality quality) {
        return new Identifier(id, type, index, quality.name());
    }
}


