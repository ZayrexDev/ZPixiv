package xyz.zcraft.zpixiv.api.artwork;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class GifData {
    @JSONField(name = "src")
    private String src;
    @JSONField(name = "originalSrc")
    private String originalSrc;
    @JSONField(name = "mime_type")
    private String mime_type;
    @JSONField(name = "frames")
    private JSONArray origFrame;
}
