package xyz.zcraft.zpixiv.api;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import xyz.zcraft.zpixiv.api.artwork.GifData;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.api.user.LoginSession;
import xyz.zcraft.zpixiv.api.user.PixivUser;

import java.io.IOException;
import java.net.Proxy;
import java.util.*;

public class PixivClient {
    private static final String TOP = "https://www.pixiv.net/ajax/top/illust?mode=all";
    private static final String RELATED = "https://www.pixiv.net/ajax/illust/%s/recommend/init?limit=%d";
    private static final String ARTWORK = "https://www.pixiv.net/artworks/";
    private static final String USER = "https://www.pixiv.net/ajax/user/%s/profile/all?";
    private static final String USER_TAGS = "https://www.pixiv.net/ajax/tags/frequent/illust?%s";
    private static final String USER_WORKS = "https://www.pixiv.net/ajax/user/%s/profile/illusts?%s&work_category=illust&is_first_page=1";
    private static final String DISCOVERY = "https://www.pixiv.net/ajax/discovery/artworks?mode=%s&limit=%d";
    private static final String GIF_DATA = "https://www.pixiv.net/ajax/illust/%s/ugoira_meta";
    private static final String PAGES = "https://www.pixiv.net/ajax/illust/%s/pages";
    private static final String SEARCH_TOP = "https://www.pixiv.net/ajax/search/top/%s";
    private static final String SEARCH_ILLUST = "https://www.pixiv.net/ajax/search/illustrations/%s?word=%s&mode=%s&p=%d";
    private static final String SEARCH_MANGA = "https://www.pixiv.net/ajax/search/manga/%s?word=%s&mode=%s&p=%d";
    private static final String RANKING = "https://www.pixiv.net/ranking.php";
    private final String cookieString;
    @Getter
    private final Proxy proxy;
    private final PixivUser userData;

    public PixivClient(String cookieString, Proxy proxy) throws IOException {
        this.proxy = proxy;
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

    public static HashMap<String, String> parseCookie(String cookieString) {
        if (cookieString == null) return new HashMap<>();
        String[] t = cookieString.split(";");
        HashMap<String, String> cookieMap = new HashMap<>();
        for (String t2 : t) {
            t2 = t2.trim();
            String[] t3 = t2.split("=");
            if (t3.length >= 2) {
                cookieMap.put(t3[0], t3[1]);
            }
        }
        return cookieMap;
    }

    public static String getPixivLanguageTag() {
        switch (Locale.getDefault().toLanguageTag().toLowerCase()) {
            case "zh-cn", "zh_cn", "zh" -> {
                return "zh";
            }
            case "zh_tw", "zh-tw" -> {
                return "zh_tw";
            }
            default -> {
                return "en";
            }
        }
    }

    public static String getArtworkPageUrl(String id) {
        return ARTWORK + id;
    }

    public static List<String> buildQueryString(Set<String> ids) {
        LinkedList<String> list = new LinkedList<>();
        StringBuilder s = new StringBuilder();
        int count = 0;
        for (String t : ids) {
            if (count >= 48) {
                s.append("&lang=").append(getPixivLanguageTag());
                list.add(s.toString());
                count = 0;
                s = new StringBuilder();
            } else {
                s.append("&ids%5B%5D=").append(t);
                count++;
            }
        }
        if (!s.isEmpty()) {
            s.append("&lang=").append(getPixivLanguageTag());
            list.add(s.toString());
        }
        return list;
    }

    public static LinkedList<PixivArtwork> parseArtworks(String jsonString, boolean classify) {
        LinkedList<PixivArtwork> artworks = new LinkedList<>();
        JSONObject bodyObject = JSONObject.parse(jsonString).getJSONObject("body");
        JSONObject tran = bodyObject.getJSONObject("tagTranslation");
        JSONArray illust = bodyObject.getJSONObject("thumbnails").getJSONArray("illust");

        for (int i = 0; i < illust.size(); i++) {
            JSONObject jsonObject = illust.getJSONObject(i);
            PixivArtwork a = jsonObject.to(PixivArtwork.class);
            a.setTranslatedTags(new LinkedHashSet<>());
            for (Object t : a.getOriginalTags()) {
                a.getTranslatedTags().add(translateTag(t.toString(), tran));
            }
            a.setOrigJson(jsonObject);
            artworks.add(a);
        }

        if (classify) classifyArtwork(artworks, bodyObject.getJSONObject("page"));

        return artworks;
    }

    public static LinkedList<PixivArtwork> parseArtworks(String jsonString) {
        LinkedList<PixivArtwork> artworks = new LinkedList<>();
        JSONObject bodyObject = JSONObject.parse(jsonString).getJSONObject("body");
        JSONObject tran = bodyObject.getJSONObject("tagTranslation");
        JSONArray illust = bodyObject.getJSONObject("thumbnails").getJSONArray("illust");

        for (int i = 0; i < illust.size(); i++) {
            JSONObject jsonObject = illust.getJSONObject(i);
            PixivArtwork a = jsonObject.to(PixivArtwork.class);
            a.setOrigJson(jsonObject);
            a.setFrom(From.Discovery);
            a.setTranslatedTags(new LinkedHashSet<>());
            for (Object t : a.getOriginalTags()) {
                a.getTranslatedTags().add(translateTag(t.toString(), tran));
            }
            artworks.add(a);
        }

        return artworks;
    }

    public static String translateTag(String tag, JSONObject tran) {
        String origLang = Locale.getDefault().toLanguageTag().toLowerCase();
        JSONObject tagObj = tran.getJSONObject(tag);
        if (tagObj == null) return tag;
        switch (origLang) {
            case "zh-cn", "zh_cn", "zh" -> {
                String s = Objects.requireNonNullElse(tagObj.getString("zh"), tag);
                return s.isEmpty() ? tag : s;
            }
            case "zh_tw", "zh-tw" -> {
                String s = Objects.requireNonNullElse(tagObj.getString("zh_tw"), tag);
                return s.isEmpty() ? tag : s;
            }
            case "en" -> {
                String s = Objects.requireNonNullElse(tagObj.getString("en"), tag);
                return s.isEmpty() ? tag : s;
            }
            default -> {
                return tag;
            }
        }
    }

    public static String getArtworkPageUrl(PixivArtwork artwork) {
        return ARTWORK + artwork.getId();
    }

    public static void classifyArtwork(List<PixivArtwork> orig, JSONObject pageJson) {
        LinkedList<String> recommendIDs = new LinkedList<>();
        LinkedList<String> recommendTagIDs = new LinkedList<>();
        LinkedList<String> recommendUserIDs = new LinkedList<>();
        LinkedList<String> followIDs = new LinkedList<>();

        for (Object o : pageJson.getJSONArray("follow")) {
            followIDs.add(o.toString());
        }

        for (Object o : pageJson.getJSONObject("recommend").getJSONArray("ids")) {
            recommendIDs.add(o.toString());
        }

        JSONArray recommendByTag = pageJson.getJSONArray("recommendByTag");
        for (int i = 0; i < recommendByTag.size(); i++) {
            for (Object ids : recommendByTag.getJSONObject(i).getJSONArray("ids")) {
                recommendTagIDs.add(ids.toString());
            }
        }

        JSONArray recommendUser = pageJson.getJSONArray("recommendUser");
        for (int i = 0; i < recommendUser.size(); i++) {
            for (Object ids : recommendUser.getJSONObject(i).getJSONArray("illustIds")) {
                recommendUserIDs.add(ids.toString());
            }
        }

        for (PixivArtwork artwork : orig) {
            String id = artwork.getId();
            if (followIDs.contains(id)) {
                artwork.setFrom(From.Follow);
            } else if (recommendIDs.contains(id)) {
                artwork.setFrom(From.Recommend);
            } else if (recommendTagIDs.contains(id)) {
                artwork.setFrom(From.RecommendTag);
            } else if (recommendUserIDs.contains(id)) {
                artwork.setFrom(From.RecommendUser);
            } else {
                artwork.setFrom(From.Other);
            }
        }
    }

    public static List<PixivArtwork> selectArtworks(List<PixivArtwork> orig, int limit, boolean follow, boolean recommend, boolean recommendTag, boolean recommendUser, boolean other) {
        LinkedList<PixivArtwork> art = new LinkedList<>();
        for (PixivArtwork artwork : orig) {
            if (limit > art.size()) {
                From from = artwork.getFrom();
                if (from == From.Other && other) art.add(artwork);
                if (from == From.Follow && follow) art.add(artwork);
                if (from == From.Recommend && recommend) art.add(artwork);
                if (from == From.RecommendTag && recommendTag) art.add(artwork);
                if (from == From.RecommendUser && recommendUser) art.add(artwork);
            } else {
                break;
            }
        }

        return art;
    }

    public LinkedList<String> getRankingIDs(String major, String minor) throws IOException {
        String url = RANKING.concat("?mode=").concat(major).concat(minor != null && !minor.isEmpty() ? "&content=".concat(minor) : "");
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(url).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);

        if (proxy != null) c.proxy(proxy);

        Elements rankingItems = c.get().body().getElementsByClass("ranking-item");
        LinkedList<String> ids = new LinkedList<>();
        for (Element rankingItem : rankingItems) {
            String href = rankingItem.getElementsByClass("ranking-image-item").get(0).getElementsByTag("a").get(0).attr("href");
            String id = href.substring(href.lastIndexOf("/") + 1);
            ids.add(id);
        }

        return ids;
    }

    public List<PixivArtwork> searchTopArtworks(String keyword) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(String.format(SEARCH_TOP, keyword).concat("?lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);

        if (proxy != null) c.proxy(proxy);

        String s = c.get().body().ownText();
        JSONObject body = JSONObject.parseObject(s).getJSONObject("body");

        JSONObject popular = body.getJSONObject("popular");

        LinkedList<PixivArtwork> artworks = new LinkedList<>();
        for (Object o : popular.getJSONArray("recent")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }
        for (Object o : popular.getJSONArray("permanent")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }
        for (Object o : body.getJSONObject("illustManga").getJSONArray("data")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }

        return artworks;
    }

    public List<PixivArtwork> searchIllustArtworks(String keyword, Mode mode, int page) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        String url = String.format(SEARCH_ILLUST, keyword, keyword, mode.argStr, page).concat("&lang=").concat(getPixivLanguageTag());
        Connection c = Jsoup.connect(url).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);

        if (proxy != null) c.proxy(proxy);

        String s = c.get().body().ownText();
        JSONObject body = JSONObject.parseObject(s).getJSONObject("body");

        JSONObject popular = body.getJSONObject("popular");

        LinkedList<PixivArtwork> artworks = new LinkedList<>();
        for (Object o : popular.getJSONArray("recent")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }
        for (Object o : popular.getJSONArray("permanent")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }
        for (Object o : body.getJSONObject("illust").getJSONArray("data")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }

        return artworks;
    }

    public List<PixivArtwork> searchMangaArtworks(String keyword, Mode mode, int page) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        String url = String.format(SEARCH_MANGA, keyword, keyword, mode.argStr, page).concat("&lang=").concat(getPixivLanguageTag());
        Connection c = Jsoup.connect(url).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);

        if (proxy != null) c.proxy(proxy);

        String s = c.get().body().ownText();
        JSONObject body = JSONObject.parseObject(s).getJSONObject("body");

        JSONObject popular = body.getJSONObject("popular");

        LinkedList<PixivArtwork> artworks = new LinkedList<>();
        for (Object o : popular.getJSONArray("recent")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }
        for (Object o : popular.getJSONArray("permanent")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }
        for (Object o : body.getJSONObject("manga").getJSONArray("data")) {
            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
            e.setSearch(keyword);
            e.setFrom(From.Search);
            artworks.add(e);
        }

        return artworks;
    }

    public GifData getGifData(PixivArtwork artwork) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(String.format(GIF_DATA, artwork.getId()).concat("?lang=").concat(getPixivLanguageTag())).ignoreContentType(true).cookies(cookie).method(Connection.Method.GET).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);
        JSONObject body = JSONObject.parseObject(c.get().body().ownText()).getJSONObject("body");
        GifData gifData = body.to(GifData.class);
        artwork.setGifData(gifData);
        return gifData;
    }

    public void getFullPages(PixivArtwork artwork) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(String.format(PAGES, artwork.getId()).concat("?lang=").concat(getPixivLanguageTag())).ignoreContentType(true).cookies(cookie).method(Connection.Method.GET).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);
        JSONArray body = JSONObject.parseObject(c.get().body().ownText()).getJSONArray("body");
        artwork.setImageUrls(new LinkedList<>());
        for (int i = 0; i < body.size(); i++) {
            artwork.getImageUrls().add(body.getJSONObject(i).getJSONObject("urls").getString("original"));
        }
    }

    public PixivArtwork getArtwork(String id) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(getArtworkPageUrl(id)).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);

        final JSONObject content = JSONObject.parseObject(Objects.requireNonNull(c.get().head().getElementById("meta-preload-data")).attr("content"));

        JSONObject illustJsonObj = content.getJSONObject("illust").getJSONObject(id);

        PixivArtwork pixivArtwork = illustJsonObj.to(PixivArtwork.class);

        pixivArtwork.setOrigJson(illustJsonObj);

        JSONArray jsonArray = illustJsonObj.getJSONObject("tags").getJSONArray("tags");
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            tags.add(jsonArray.getJSONObject(i).getString("tag"));
        }
        pixivArtwork.setTranslatedTags(tags);

        final JSONObject userJsonObj = content.getJSONObject("user").getJSONObject(pixivArtwork.getUserId());
        pixivArtwork.setAuthor(userJsonObj.to(PixivUser.class));

        return pixivArtwork;
    }

    public void getFullData(PixivArtwork artwork) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(getArtworkPageUrl(artwork.getId())).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);

        final JSONObject content = JSONObject.parseObject(Objects.requireNonNull(c.get().head().getElementById("meta-preload-data")).attr("content"));
        JSONObject jsonObject = content.getJSONObject("illust").getJSONObject(artwork.getId());

        artwork.setBookmarkCount(jsonObject.getInteger("bookmarkCount"));
        artwork.setLikeCount(jsonObject.getInteger("likeCount"));

        final JSONObject userJsonObj = content.getJSONObject("user").getJSONObject(artwork.getUserId());
        artwork.setAuthor(userJsonObj.to(PixivUser.class));
    }

    public List<PixivArtwork> getDiscovery(Mode mode, int limit) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(String.format(DISCOVERY, mode.argStr, limit).concat("&lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);

        if (proxy != null) c.proxy(proxy);

        return parseArtworks(c.get().body().ownText());
    }

    public Set<String> fetchUser(String uid) throws IOException {
        Connection c = Jsoup.connect(String.format(USER, uid).concat("lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);

        JSONObject jsonObject = JSONObject.parseObject(c.get().body().ownText()).getJSONObject("body").getJSONObject("illusts");

        return new HashSet<>(jsonObject.keySet());
    }

    public HashMap<String, String> getUserTagTranslations(String queryString) throws IOException {
        HashMap<String, String> tagTranslation = new HashMap<>();
        Connection c = Jsoup.connect(String.format(USER_TAGS, queryString)).ignoreContentType(true).method(Connection.Method.GET).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);


        String s = c.get().body().ownText();
        for (Object o : JSONObject.parseObject(s).getJSONArray("body")) {
            if (o instanceof JSONObject obj) {
                tagTranslation.put(obj.getString("tag"), obj.getString("tag_translation"));
            }
        }

        return tagTranslation;
    }

    public List<PixivArtwork> getUserArtworks(String queryString, String uid) throws IOException {
        LinkedList<PixivArtwork> artworks = new LinkedList<>();
        Connection c = Jsoup.connect(String.format(USER_WORKS, uid, queryString)).ignoreContentType(true).method(Connection.Method.GET).timeout(10 * 1000);

        if (proxy != null) c.proxy(proxy);

        JSONObject obj = JSONObject.parseObject(c.get().body().ownText()).getJSONObject("body").getJSONObject("works");

        HashMap<String, String> userTagTranslations = getUserTagTranslations(queryString);
        LinkedHashSet<String> translatedTags = new LinkedHashSet<>();
        for (String k : obj.keySet()) {
            JSONObject jsonObject = obj.getJSONObject(k);
            PixivArtwork a = jsonObject.to(PixivArtwork.class);
            for (Object originalTag : a.getOriginalTags()) {
                String s = Objects.requireNonNullElse(userTagTranslations.get(originalTag.toString()), originalTag.toString());
                translatedTags.add(s);
            }
            a.setTranslatedTags(translatedTags);
            a.setFrom(From.User);
            a.setOrigJson(jsonObject);
            artworks.add(a);
        }

        return artworks;
    }

    public List<PixivArtwork> fetchMenu() throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(TOP.concat("&lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);

        return parseArtworks(c.get().body().ownText(), true);
    }

    public List<PixivArtwork> getRelated(PixivArtwork artwork, int limit) throws IOException {
        HashMap<String, String> cookie = parseCookie(cookieString);
        Connection c = Jsoup.connect(String.format(RELATED, artwork.getId(), limit)).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
        if (proxy != null) c.proxy(proxy);

        String jsonString = c.get().body().ownText();

        LinkedList<PixivArtwork> artworks = new LinkedList<>();

        JSONArray illusts = JSONObject.parse(jsonString).getJSONObject("body").getJSONArray("illusts");

        for (int i = 0; i < illusts.size(); i++) {
            JSONObject jsonObject = illusts.getJSONObject(i);
            PixivArtwork object = jsonObject.to(PixivArtwork.class);
            if (object.getTitle() == null) continue;
            object.setOrigJson(jsonObject);
            object.setFrom(From.Related);
            artworks.add(object);
        }

        return artworks;
    }

    @AllArgsConstructor
    public enum Mode {
        ALL("all"), SAFE("save"), R18("r18");

        private final String argStr;
    }

    public enum From {
        Follow, Recommend, RecommendUser, RecommendTag, Related, Spec, Discovery, Ranking, Search, User, Other
    }
}