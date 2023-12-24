package xyz.zcraft.zpixiv.api.artwork;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class BgSlideArtwork {
    @JSONField(name = "illust_id")
    private String id;
    @JSONField(name = "illust_title")
    private String title;
    @JSONField(name = "url")
    private JSONObject url;
    @JSONField(name = "user_name")
    private String userName;
    @JSONField(name = "profile_img")
    private JSONObject profileImg;
    @JSONField(name = "www_member_illust_medium_url")
    private String memIllUrl;
    @JSONField(name = "www_user_url")
    private String userUrl;
}
