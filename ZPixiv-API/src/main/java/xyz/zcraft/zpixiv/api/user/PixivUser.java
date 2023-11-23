package xyz.zcraft.zpixiv.api.user;

import com.alibaba.fastjson2.annotation.JSONField;

@lombok.Data
public class PixivUser {
    @JSONField(name = "id")
    private String id;
    @JSONField(name = "name")
    private String name;
    @JSONField(name = "pixivid")
    private String pixivId;
    @JSONField(name = "profileImg")
    private String profileImg;
    @JSONField(name = "profileImgBig")
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

    private String token;
}