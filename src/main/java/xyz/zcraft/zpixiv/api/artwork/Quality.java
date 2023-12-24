package xyz.zcraft.zpixiv.api.artwork;

import lombok.Getter;

@Getter
public enum Quality {
    ThumbMini("thumb_mini"), Small("small"), Regular("regular"), Original("original");

    private final String str;
    Quality(String str) {
        this.str = str;
    }
}
