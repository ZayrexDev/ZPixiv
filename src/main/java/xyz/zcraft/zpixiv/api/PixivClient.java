package xyz.zcraft.zpixiv.api;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import xyz.zcraft.zpixiv.api.artwork.BgSlideArtwork;
import xyz.zcraft.zpixiv.api.artwork.GifData;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.api.user.PixivUser;

import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static xyz.zcraft.zpixiv.api.PixivClient.Urls.DISCOVERY;
import static xyz.zcraft.zpixiv.api.PixivClient.Urls.USER_TOP;

@SuppressWarnings("unused")
public class PixivClient {
    private static final Logger LOG = LogManager.getLogger(PixivClient.class);
    private static final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0";
    @Getter
    private final PixivUser userData;
    private final HashMap<String, String> cookie;
    @Getter
    @Setter
    private Proxy proxy;

    public PixivClient(String cookieString, Proxy proxy, boolean getUserData) throws IOException {
        cookie = parseCookie(cookieString);
        this.proxy = proxy;
        if (getUserData) {
            final Connection c = getConnection(Urls.MAIN, Connection.Method.GET);
            LOG.info("Getting user info");
            String text = Objects.requireNonNull(c.get().getElementById("meta-global-data")).attr("content");
            final JSONObject jsonObject = JSONObject.parseObject(text);
            JSONObject userDataJson = jsonObject.getJSONObject("userData");

            if (userDataJson == null) userData = null;
            else {
                PixivUser userData = userDataJson.to(PixivUser.class);
                userData.setToken(jsonObject.getString("token"));
                if (userData.getName() != null && userData.getId() != null) {
                    this.userData = userData;
                } else {
                    throw new RuntimeException("Can't login");
                }
            }
        } else {
            userData = null;
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

    public static String getArtworkPageUrl(String id) {
        return Urls.ARTWORK + id;
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

    public List<BgSlideArtwork> getLoginBackground() throws IOException {
        final var c = Jsoup.connect(Urls.MAIN)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .method(Connection.Method.GET)
                .timeout(10 * 1000);
        setConnectionProxy(c);

        final String orig = Objects.requireNonNull(c.get().getElementById("init-config")).attr("value");
        final JSONObject obj = JSONObject.parseObject(orig.replace("&quot;", "\""));

        final JSONArray jsonArray = obj.getJSONObject("pixivBackgroundSlideshow.illusts").getJSONArray("landscape");
        final LinkedList<BgSlideArtwork> result = new LinkedList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            result.add(jsonArray.getJSONObject(i).to(BgSlideArtwork.class));
        }
        return result;
    }

    public void followUser(PixivUser user) throws IOException {
        final var c = getConnection(Urls.FOLLOW_USER, Connection.Method.POST);

        String requestBuilder = "mode=add&type=user&user_id=" +
                user.getId() +
                "&tag=&restrict=0&format=json";
        c.requestBody(requestBuilder);

        c.post();
    }

    public void getGifData(PixivArtwork artwork) throws IOException {
        final var c = getConnection(
                String.format(Urls.GIF_DATA, artwork.getOrigData().getId())
                        .concat("?lang=")
                        .concat(getPixivLanguageTag()),
                Connection.Method.GET
        );
        JSONObject body = JSONObject.parseObject(c.get().body().ownText()).getJSONObject("body");
        artwork.setGifData(body.to(GifData.class));
    }

    private Connection getConnection(String url, Connection.Method method) {
        final var c = Jsoup.connect(url)
                .cookies(cookie)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .method(method)
                .timeout(10 * 1000)
                .header("Origin", "https://www.pixiv.net");
        if (userData != null)
            c.header("X-Csrf-Token", userData.getToken());
        setConnectionProxy(c);
        return c;
    }

    private void setConnectionProxy(Connection c) {
        if (proxy != null) c.proxy(proxy);
    }

    public void getFullPages(PixivArtwork artwork) throws IOException {
        final Connection c = getConnection(String.format(Urls.PAGES, artwork.getOrigData().getId()).concat("?lang=").concat(getPixivLanguageTag()), Connection.Method.GET);
        JSONArray body = JSONObject.parseObject(c.get().body().ownText()).getJSONArray("body");
        artwork.setImageUrls(new LinkedList<>());
        for (int i = 0; i < body.size(); i++) {
            artwork.getImageUrls().add(body.getJSONObject(i).getJSONObject("urls"));
        }
    }

    public PixivArtwork getArtwork(String id) throws IOException {
        final var c = getConnection(getArtworkPageUrl(id), Connection.Method.GET);
        final JSONObject content = JSONObject.parseObject(Objects.requireNonNull(c.get().head().getElementById("meta-preload-data")).attr("content"));

        final JSONObject illustJsonObj = content.getJSONObject("illust").getJSONObject(id);

        final PixivArtwork.OrigData pixivArtworkOrig = illustJsonObj.to(PixivArtwork.OrigData.class);
        final PixivArtwork pixivArtwork = new PixivArtwork(pixivArtworkOrig);

        pixivArtwork.setOrigJson(illustJsonObj);

        final JSONArray jsonArray = illustJsonObj.getJSONObject("tags").getJSONArray("tags");
        LinkedHashSet<PixivArtwork.Tag> tags = new LinkedHashSet<>();
        for (int i = 0; i < jsonArray.size(); i++) {
//            tags.add(jsonArray.getJSONObject(i).getString("tag"));
            final JSONObject curTagObj = jsonArray.getJSONObject(i);
            final String orig = curTagObj.getString("tag");
            String trans = null;
            final JSONObject transObj = curTagObj.getJSONObject("translation");

            if (transObj != null) {
                if (transObj.getString(getPixivLanguageTag()) != null)
                    trans = transObj.getString(getPixivLanguageTag());
                else trans = transObj.getString("en");
            }
            tags.add(new PixivArtwork.Tag(orig, trans));
        }
        pixivArtwork.setTags(tags);

        final JSONObject userJsonObj = content.getJSONObject("user").getJSONObject(pixivArtwork.getOrigData().getUserId());
        pixivArtwork.setAuthor(userJsonObj.to(PixivUser.class));

        if(pixivArtwork.isGif()) {
            getGifData(pixivArtwork);
        }

        return pixivArtwork;
    }

    public boolean likeArtwork(PixivArtwork artwork) throws IOException {
        LOG.info("Adding like to {}", artwork.getOrigData().getId());
        final JSONObject obj = new JSONObject();
        obj.put("illust_id", artwork.getOrigData().getId());

        final String requestBody = obj.toString();

        final var c = getConnection(Urls.LIKE, Connection.Method.POST)
                .header("Accept", "application/json")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Content-Length", String.valueOf(requestBody.length()))
                .requestBody(requestBody);
        setConnectionProxy(c);

        final JSONObject response = JSONObject.parseObject(new String(c.execute().body().getBytes(StandardCharsets.UTF_8)));
        if (!response.getBoolean("error")) {
            artwork.getOrigData().setLiked(true);
            return true;
        } else {
            return false;
        }
    }

    public List<PixivArtwork> getDiscovery(Mode mode, int limit) throws IOException {
        LOG.info("Getting discovery");
        Connection c = getConnection(String.format(DISCOVERY, mode.argStr, limit).concat("&lang=").concat(getPixivLanguageTag()), Connection.Method.GET);

        final String jsonString = c.get().body().ownText();

        final LinkedList<PixivArtwork> artworks = new LinkedList<>();
        final JSONObject bodyObject = JSONObject.parse(jsonString).getJSONObject("body");
        final JSONObject tran = bodyObject.getJSONObject("tagTranslation");
        final JSONArray illust = bodyObject.getJSONObject("thumbnails").getJSONArray("illust");

        for (int i = 0; i < illust.size(); i++) {
            final JSONObject jsonObject = illust.getJSONObject(i);
            final PixivArtwork.OrigData origData = jsonObject.to(PixivArtwork.OrigData.class);
            final PixivArtwork artwork = new PixivArtwork(origData);
            artwork.setOrigJson(jsonObject);
            artwork.setTags(new LinkedHashSet<>());
            for (Object t : artwork.getOrigData().getOriginalTags()) {
                final String str = (String) t;
                artwork.getTags().add(new PixivArtwork.Tag(str, translateTag(str, tran)));
            }
            artworks.add(artwork);
        }

        LOG.info("Got {} artworks.", illust.size());

        return artworks;
    }

    public boolean addBookmark(PixivArtwork artwork) throws IOException {
        LOG.info("Adding bm to {}", artwork.getOrigData().getId());
        final JSONObject obj = new JSONObject();
        obj.put("illust_id", artwork.getOrigData().getId());
        obj.put("restrict", 0);
        obj.put("comment", "");
        obj.put("tags", new JSONArray());

        final String requestBody = obj.toString();

        final var c = getConnection(Urls.ADD_BOOKMARK, Connection.Method.POST)
                .header("Accept", "application/json")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Content-Length", String.valueOf(requestBody.length()))
                .requestBody(requestBody);
        setConnectionProxy(c);

        final JSONObject response = JSONObject.parseObject(new String(c.execute().body().getBytes(StandardCharsets.UTF_8)));
        if (!response.getBoolean("error")) {
            final JSONObject bmData = new JSONObject();
            bmData.put("id", response.getJSONObject("body").getString("last_bookmark_id"));
            bmData.put("private", false);
            artwork.getOrigData().setBookmarkData(bmData);
            return true;
        } else return false;
    }

    public PixivArtwork getFullData(PixivArtwork artwork) throws IOException {
        return getArtwork(artwork.getOrigData().getId());
    }

    public List<PixivArtwork> getUserTopArtworks(String id) throws IOException {
        LOG.info("Getting user {} top artworks", id);
        Connection c = getConnection(String.format(USER_TOP, id).concat("&lang=").concat(getPixivLanguageTag()), Connection.Method.GET);

        final String responseStr = c.get().body().ownText();
        final JSONObject response = JSONObject.parseObject(responseStr);

        final LinkedList<PixivArtwork> artworks = new LinkedList<>();
        final JSONObject body = response.getJSONObject("body");
        body.getJSONObject("illusts").forEach((s, o) -> artworks.add(new PixivArtwork(((JSONObject)o).to(PixivArtwork.OrigData.class))));
        body.getJSONObject("manga").forEach((s, o) -> artworks.add(new PixivArtwork(((JSONObject)o).to(PixivArtwork.OrigData.class))));

        artworks.sort(Comparator.comparingInt(o -> Integer.parseInt(o.getOrigData().getId())));

        return artworks;
    }

//    public static List<String> buildQueryString(Set<String> ids) {
//        LinkedList<String> list = new LinkedList<>();
//        StringBuilder s = new StringBuilder();
//        int count = 0;
//        for (String t : ids) {
//            if (count >= 48) {
//                s.append("&lang=").append(getPixivLanguageTag());
//                list.add(s.toString());
//                count = 0;
//                s = new StringBuilder();
//            } else {
//                s.append("&ids%5B%5D=").append(t);
//                count++;
//            }
//        }
//        if (!s.isEmpty()) {
//            s.append("&lang=").append(getPixivLanguageTag());
//            list.add(s.toString());
//        }
//
//        return list;
//    }

//    public static String getArtworkPageUrl(PixivArtwork artwork) {
//        return ARTWORK + artwork.getId();
//    }

//    public static void classifyArtwork(List<PixivArtwork> orig, JSONObject pageJson) {
//        LinkedList<String> recommendIDs = new LinkedList<>();
//        LinkedList<String> recommendTagIDs = new LinkedList<>();
//        LinkedList<String> recommendUserIDs = new LinkedList<>();
//        LinkedList<String> followIDs = new LinkedList<>();
//
//        for (Object o : pageJson.getJSONArray("follow")) {
//            followIDs.add(o.toString());
//        }
//
//        for (Object o : pageJson.getJSONObject("recommend").getJSONArray("ids")) {
//            recommendIDs.add(o.toString());
//        }
//
//        JSONArray recommendByTag = pageJson.getJSONArray("recommendByTag");
//        for (int i = 0; i < recommendByTag.size(); i++) {
//            for (Object ids : recommendByTag.getJSONObject(i).getJSONArray("ids")) {
//                recommendTagIDs.add(ids.toString());
//            }
//        }
//
//        JSONArray recommendUser = pageJson.getJSONArray("recommendUser");
//        for (int i = 0; i < recommendUser.size(); i++) {
//            for (Object ids : recommendUser.getJSONObject(i).getJSONArray("illustIds")) {
//                recommendUserIDs.add(ids.toString());
//            }
//        }
//
//        for (PixivArtwork artwork : orig) {
//            String id = artwork.getId();
//            if (followIDs.contains(id)) {
//                artwork.setFrom(From.Follow);
//            } else if (recommendIDs.contains(id)) {
//                artwork.setFrom(From.Recommend);
//            } else if (recommendTagIDs.contains(id)) {
//                artwork.setFrom(From.RecommendTag);
//            } else if (recommendUserIDs.contains(id)) {
//                artwork.setFrom(From.RecommendUser);
//            } else {
//                artwork.setFrom(From.Other);
//            }
//        }

//    }

//    public static List<PixivArtwork> selectArtworks(List<PixivArtwork> orig, int limit, boolean follow, boolean recommend, boolean recommendTag, boolean recommendUser, boolean other) {
//        LinkedList<PixivArtwork> art = new LinkedList<>();
//        for (PixivArtwork artwork : orig) {
//            if (limit > art.size()) {
//                From from = artwork.getFrom();
//                if (from == From.Other && other) art.add(artwork);
//                if (from == From.Follow && follow) art.add(artwork);
//                if (from == From.Recommend && recommend) art.add(artwork);
//                if (from == From.RecommendTag && recommendTag) art.add(artwork);
//                if (from == From.RecommendUser && recommendUser) art.add(artwork);
//            } else {
//                break;
//            }
//        }
//        return art;
//    }

//    public LinkedList<String> getRankingIDs(String major, String minor) throws IOException {
//        String url = RANKING.concat("?mode=").concat(major).concat(minor != null && !minor.isEmpty() ? "&content=".concat(minor) : "");
//        HashMap<String, String> cookie = parseCookie(cookieString);
//        Connection c = Jsoup.connect(url).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
//
//        if (proxy != null) c.proxy(proxy);
//
//        Elements rankingItems = c.get().body().getElementsByClass("ranking-item");
//        LinkedList<String> ids = new LinkedList<>();
//        for (Element rankingItem : rankingItems) {
//            String href = rankingItem.getElementsByClass("ranking-image-item").get(0).getElementsByTag("a").get(0).attr("href");
//            String id = href.substring(href.lastIndexOf("/") + 1);
//            ids.add(id);
//        }
//
//        return ids;
//    }

//    public List<PixivArtwork> searchTopArtworks(String keyword) throws IOException {
//        HashMap<String, String> cookie = parseCookie(cookieString);
//        Connection c = Jsoup.connect(String.format(SEARCH_TOP, keyword).concat("?lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
//
//        if (proxy != null) c.proxy(proxy);
//
//        String s = c.get().body().ownText();
//        JSONObject body = JSONObject.parseObject(s).getJSONObject("body");
//
//        JSONObject popular = body.getJSONObject("popular");
//
//        LinkedList<PixivArtwork> artworks = new LinkedList<>();
//        for (Object o : popular.getJSONArray("recent")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        for (Object o : popular.getJSONArray("permanent")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        for (Object o : body.getJSONObject("illustManga").getJSONArray("data")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        return artworks;
//    }
//    public List<PixivArtwork> searchIllustArtworks(String keyword, Mode mode, int page) throws IOException {
//        HashMap<String, String> cookie = parseCookie(cookieString);
//        String url = String.format(SEARCH_ILLUST, keyword, keyword, mode.argStr, page).concat("&lang=").concat(getPixivLanguageTag());
//        Connection c = Jsoup.connect(url).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
//
//        if (proxy != null) c.proxy(proxy);
//
//        String s = c.get().body().ownText();
//        JSONObject body = JSONObject.parseObject(s).getJSONObject("body");
//
//        JSONObject popular = body.getJSONObject("popular");
//
//        LinkedList<PixivArtwork> artworks = new LinkedList<>();
//        for (Object o : popular.getJSONArray("recent")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        for (Object o : popular.getJSONArray("permanent")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        for (Object o : body.getJSONObject("illust").getJSONArray("data")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//
//        return artworks;
//    }
//    public List<PixivArtwork> searchMangaArtworks(String keyword, Mode mode, int page) throws IOException {
//        HashMap<String, String> cookie = parseCookie(cookieString);
//        String url = String.format(SEARCH_MANGA, keyword, keyword, mode.argStr, page).concat("&lang=").concat(getPixivLanguageTag());
//        Connection c = Jsoup.connect(url).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
//
//        if (proxy != null) c.proxy(proxy);
//
//        String s = c.get().body().ownText();
//        JSONObject body = JSONObject.parseObject(s).getJSONObject("body");
//
//        JSONObject popular = body.getJSONObject("popular");
//
//        LinkedList<PixivArtwork> artworks = new LinkedList<>();
//        for (Object o : popular.getJSONArray("recent")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        for (Object o : popular.getJSONArray("permanent")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//        for (Object o : body.getJSONObject("manga").getJSONArray("data")) {
//            PixivArtwork e = JSONObject.parseObject(o.toString(), PixivArtwork.class);
//            e.setSearch(keyword);
//            e.setFrom(From.Search);
//            artworks.add(e);
//        }
//
//        return artworks;
//    }

//    public Set<String> fetchUser(String uid) throws IOException {
//        Connection c = Jsoup.connect(String.format(USER, uid).concat("lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).timeout(10 * 1000);
//        if (proxy != null) c.proxy(proxy);
//
//        JSONObject jsonObject = JSONObject.parseObject(c.get().body().ownText()).getJSONObject("body").getJSONObject("illusts");
//
//        return new HashSet<>(jsonObject.keySet());
//    }

//    public HashMap<String, String> getUserTagTranslations(String queryString) throws IOException {
//        HashMap<String, String> tagTranslation = new HashMap<>();
//        Connection c = Jsoup.connect(String.format(USER_TAGS, queryString)).ignoreContentType(true).method(Connection.Method.GET).timeout(10 * 1000);
//        if (proxy != null) c.proxy(proxy);
//
//
//        String s = c.get().body().ownText();
//        for (Object o : JSONObject.parseObject(s).getJSONArray("body")) {
//            if (o instanceof JSONObject obj) {
//                tagTranslation.put(obj.getString("tag"), obj.getString("tag_translation"));
//            }
//        }
//
//        return tagTranslation;
//    }

//    public List<PixivArtwork> getUserArtworks(String queryString, String uid) throws IOException {
//        LinkedList<PixivArtwork> artworks = new LinkedList<>();
//        Connection c = Jsoup.connect(String.format(USER_WORKS, uid, queryString)).ignoreContentType(true).method(Connection.Method.GET).timeout(10 * 1000);
//
//        if (proxy != null) c.proxy(proxy);
//
//        JSONObject obj = JSONObject.parseObject(c.get().body().ownText()).getJSONObject("body").getJSONObject("works");
//
//        HashMap<String, String> userTagTranslations = getUserTagTranslations(queryString);
//        LinkedHashSet<String> translatedTags = new LinkedHashSet<>();
//        for (String k : obj.keySet()) {
//            JSONObject jsonObject = obj.getJSONObject(k);
//            PixivArtwork a = jsonObject.to(PixivArtwork.class);
//            for (Object originalTag : a.getOriginalTags()) {
//                String s = Objects.requireNonNullElse(userTagTranslations.get(originalTag.toString()), originalTag.toString());
//                translatedTags.add(s);
//            }
//            a.setTranslatedTags(translatedTags);
//            a.setFrom(From.User);
//            a.setOrigJson(jsonObject);
//            artworks.add(a);
//        }
//
//        return artworks;
//    }

//    public List<PixivArtwork> fetchMenu() throws IOException {
//        HashMap<String, String> cookie = parseCookie(cookieString);
//        Connection c = Jsoup.connect(TOP.concat("&lang=").concat(getPixivLanguageTag())).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
//        if (proxy != null) c.proxy(proxy);
//
//        final String jsonString = c.get().body().ownText();
//
//        LinkedList<PixivArtwork> artworks = new LinkedList<>();
//        JSONObject bodyObject = JSONObject.parse(jsonString).getJSONObject("body");
//        JSONObject tran = bodyObject.getJSONObject("tagTranslation");
//        JSONArray illust = bodyObject.getJSONObject("thumbnails").getJSONArray("illust");
//
//        for (int i = 0; i < illust.size(); i++) {
//            JSONObject jsonObject = illust.getJSONObject(i);
//            PixivArtwork a = jsonObject.to(PixivArtwork.class);
//            a.setTranslatedTags(new LinkedHashSet<>());
//            for (Object t : a.getOriginalTags()) {
//                a.getTranslatedTags().add(translateTag(t.toString(), tran));
//            }
//            a.setOrigJson(jsonObject);
//            artworks.add(a);
//        }
//
//        classifyArtwork(artworks, bodyObject.getJSONObject("page"));
//
//        return artworks;
//    }

//    public List<PixivArtwork> getRelated(PixivArtwork artwork, int limit) throws IOException {
//        HashMap<String, String> cookie = parseCookie(cookieString);
//        Connection c = Jsoup.connect(String.format(RELATED, artwork.getId(), limit)).ignoreContentType(true).method(Connection.Method.GET).cookies(cookie).timeout(10 * 1000);
//        if (proxy != null) c.proxy(proxy);
//
//        String jsonString = c.get().body().ownText();
//
//        LinkedList<PixivArtwork> artworks = new LinkedList<>();
//
//        JSONArray illusts = JSONObject.parse(jsonString).getJSONObject("body").getJSONArray("illusts");
//
//        for (int i = 0; i < illusts.size(); i++) {
//            JSONObject jsonObject = illusts.getJSONObject(i);
//            PixivArtwork object = jsonObject.to(PixivArtwork.class);
//            if (object.getTitle() == null) continue;
//            object.setOrigJson(jsonObject);
//            object.setFrom(From.Related);
//            artworks.add(object);
//        }
//
//        return artworks;
//    }

    @AllArgsConstructor
    public enum Mode {
        ALL("all"), SAFE("safe"), R18("r18");

        private final String argStr;
    }

    public enum From {
        Follow, Recommend, RecommendUser, RecommendTag, Related, Spec, Discovery, Ranking, Search, User, Other
    }

    public static class Urls {
        public static final String MAIN = "https://www.pixiv.net/";
        public static final String REFERER = "https://www.pixiv.net/";
        public static final String TOP = "https://www.pixiv.net/ajax/top/illust?mode=all";
        public static final String ADD_BOOKMARK = "https://www.pixiv.net/ajax/illusts/bookmarks/add";
        public static final String FOLLOW_USER = "https://www.pixiv.net/bookmark_add.php";
        public static final String LIKE = "https://www.pixiv.net/ajax/illusts/like";
        public static final String RELATED = "https://www.pixiv.net/ajax/illust/%s/recommend/init?limit=%d";
        public static final String ARTWORK = "https://www.pixiv.net/artworks/";
        public static final String USER = "https://www.pixiv.net/ajax/user/%s/profile/all?";
        public static final String USER_TOP = "https://www.pixiv.net/ajax/user/%s/profile/top?";
        public static final String USER_TAGS = "https://www.pixiv.net/ajax/tags/frequent/illust?%s";
        public static final String USER_WORKS = "https://www.pixiv.net/ajax/user/%s/profile/illusts?%s&work_category=illust&is_first_page=1";
        public static final String DISCOVERY = "https://www.pixiv.net/ajax/discovery/artworks?mode=%s&limit=%d";
        public static final String GIF_DATA = "https://www.pixiv.net/ajax/illust/%s/ugoira_meta";
        public static final String PAGES = "https://www.pixiv.net/ajax/illust/%s/pages";
        public static final String SEARCH_TOP = "https://www.pixiv.net/ajax/search/top/%s";
        public static final String SEARCH_ILLUST = "https://www.pixiv.net/ajax/search/illustrations/%s?word=%s&mode=%s&p=%d";
        public static final String SEARCH_MANGA = "https://www.pixiv.net/ajax/search/manga/%s?word=%s&mode=%s&p=%d";
        public static final String RANKING = "https://www.pixiv.net/ranking.php";
    }
}
