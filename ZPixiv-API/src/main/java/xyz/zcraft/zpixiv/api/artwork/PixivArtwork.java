package xyz.zcraft.zpixiv.api.artwork;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import xyz.zcraft.zpixiv.api.user.PixivUser;

import java.util.List;
import java.util.Set;

@Data
public class PixivArtwork {
    private final OrigData origData;

    private Set<Tag> tags;
    private List<String> imageUrls;
    private GifData gifData;
//    private PixivClient.From from;

    private JSONObject origJson;
//    private String search;

    private PixivUser author;

    public boolean isBookmarked() {
        return origData.bookmarkData != null;
    }

    public boolean isGif() {
        return origData.illustType == 2;
    }

    public record Tag(String orig, String trans) {
    }

    @Data
    public static class OrigData {
        @JSONField(name = "id")
        String id;
        @JSONField(name = "title")
        String title;
        @JSONField(name = "illustType")
        int illustType;
        @JSONField(name = "xRestrict")
        int xRestrict;
        @JSONField(name = "restrict")
        int restrict;
        @JSONField(name = "sl")
        int sl;
        @JSONField(name = "url")
        String url;
        @JSONField(name = "description")
        String description;
        @JSONField(name = "tags")
        JSONArray originalTags;
        @JSONField(name = "userId")
        String userId;
        @JSONField(name = "userName")
        String userName;
        @JSONField(name = "width")
        int width;
        @JSONField(name = "height")
        int height;
        @JSONField(name = "pageCount")
        int pageCount;
        @JSONField(name = "isBookmarkable")
        boolean bookmarkable;
        @JSONField(name = "bookmarkData")
        JSONObject bookmarkData;
        @JSONField(name = "alt")
        String alt;
        @JSONField(name = "titleCaptionTranslation")
        JSONObject titleCaptionTranslation;
        @JSONField(name = "createDate")
        String createDate;
        @JSONField(name = "updateDate")
        String updateDate;
        @JSONField(name = "isUnlisted")
        boolean unlisted;
        @JSONField(name = "isMasked")
        boolean masked;
        @JSONField(name = "urls")
        JSONObject urls;
        @JSONField(name = "profileImageUrl")
        String profileImageUrl;
        @JSONField(name = "aiType")
        int aiType;
        @JSONField(name = "bookmarkCount")
        int bookmarkCount;
        @JSONField(name = "likeCount")
        int likeCount;
        @JSONField(name = "viewCount")
        int viewCount;
        @JSONField(name = "likeData")
        boolean liked;
    }
}
