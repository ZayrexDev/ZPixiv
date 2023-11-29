package xyz.zcraft.zpixiv.api.user;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Objects;

import static xyz.zcraft.zpixiv.api.PixivClient.parseCookie;

@Getter
public class LoginSession {
    private final static Logger LOG = LogManager.getLogger("LoginSession");
    private final PixivUser userData;
    private final String cookieString;

    public LoginSession(String cookieString, Proxy proxy) throws IOException {
        this.cookieString = cookieString;
        HashMap<String, String> cookieMap = parseCookie(cookieString);
        Connection c = Jsoup.connect("https://www.pixiv.net").ignoreContentType(true).method(Connection.Method.GET).cookies(cookieMap).timeout(10 * 1000);
        if(proxy != null) c.proxy(proxy);
        String text = Objects.requireNonNull(c.get().getElementById("meta-global-data")).attr("content");
        final JSONObject jsonObject = JSONObject.parseObject(text);
        PixivUser userData = jsonObject.getJSONObject("userData").to(PixivUser.class);
        userData.setToken(jsonObject.getString("token"));
        if (userData.getName() != null && userData.getId() != null) {
            this.userData = userData;
        } else {
            throw new RuntimeException("Can't login");
        }
    }
}
