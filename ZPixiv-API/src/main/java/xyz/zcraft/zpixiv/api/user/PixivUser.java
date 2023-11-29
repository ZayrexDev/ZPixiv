package xyz.zcraft.zpixiv.api.user;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;

@lombok.Data
public class PixivUser {
    @JSONField(alternateNames = {"id", "userId"})
    private String id;
    @JSONField(name = "name")
    private String name;
    @JSONField(name = "pixivId")
    private String pixivId;
    @JSONField(alternateNames = {"profileImg", "image"})
    private String profileImg;
    @JSONField(alternateNames = {"profileImgBig", "imageBig"})
    private String profileImgBig;
    @JSONField(name = "premium")
    private boolean premium;
    @JSONField(name = "xRestrict")
    private int xRestrict;
    @JSONField(name = "adult")
    private boolean adult;
    @JSONField(name = "safeMode")
    private boolean safeMode;
    @JSONField(name = "illustCreator")
    private boolean illustCreator;
    @JSONField(name = "novelCreator")
    private boolean novelCreator;
    @JSONField(name = "hideAiWorks")
    private boolean hideAiWorks;
    @JSONField(name = "readingStatusEnabled")
    private boolean readingStatusEnabled;
    @JSONField(name = "background")
    private JSONObject background;

    private String token;
}