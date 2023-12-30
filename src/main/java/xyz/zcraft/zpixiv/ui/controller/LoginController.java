package xyz.zcraft.zpixiv.ui.controller;

import com.alibaba.fastjson2.JSONWriter;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.BgSlideArtwork;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.CachedImage;
import xyz.zcraft.zpixiv.util.Identifier;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;

public class LoginController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(LoginController.class);
    private static List<BgSlideArtwork> loginBgOrig;
    private final Timer timer = new Timer();
    public TextArea cookieField;
    public ImageView bgAuthorImg;
    public ImageView bgImg;
    public Label bgTitleLbl;
    public Label bgAuthorLbl;
    public AnchorPane root;
    public CachedImage curImg;

//    private void setBackground() {
//        if (curImg == null) return;
//        Rectangle2D vp;
//        final double w = root.getWidth();
//        final double h = root.getHeight();
//        final double imgW = curImg.getImage().getWidth();
//        final double imgH = curImg.getImage().getHeight();
//        if (imgW / imgH > w / h) {
//            bgImg.setFitHeight(h);
//            bgImg.setFitWidth(0);
//            vp = new Rectangle2D((imgW - (imgH / h * w)) / 2, 0, imgW, imgH);
//        } else {
//            bgImg.setFitWidth(w);
//            bgImg.setFitHeight(0);
//            vp = new Rectangle2D(0, (imgH - imgW / w * h) / 2, imgW, imgH);
//        }
//
//        bgImg.setImage(curImg.getImage());
//        bgImg.setViewport(vp);
//    }

    private void loadLoginBg() {
        try {
            Proxy p = null;
            if (Main.getConfig().getProxyHost() != null && Main.getConfig().getProxyPort() != null) {
                p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Main.getConfig().getProxyPort(), Integer.parseInt(Main.getConfig().getProxyHost())));
            }
            loginBgOrig = new PixivClient(null, p, false).getLoginBackground();
        } catch (IOException e) {
            LOG.error("Failed to get login backgrounds.", e);
        }
    }

    private void switchBackground() {
        Main.getTpe().submit(() -> {
            try {
                BgSlideArtwork bgSlideArtwork = loginBgOrig.get(new Random().nextInt(loginBgOrig.size()));
                URL url = new URL(bgSlideArtwork.getUrl().getString("medium"));
                LOG.info(bgSlideArtwork.getProfileImg().toString(JSONWriter.Feature.PrettyFormat));
                URLConnection c;

                Proxy p;
                if (Main.getConfig().getProxyHost() != null && Main.getConfig().getProxyPort() != null) {
                    p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(Main.getConfig().getProxyPort(), Integer.parseInt(Main.getConfig().getProxyHost())));
                    c = url.openConnection(p);
                } else {
                    c = url.openConnection();
                }

                c.setRequestProperty("Referer", "https://www.pixiv.net");

                Identifier identifier = Identifier.of(bgSlideArtwork.getId(), Identifier.Type.Artwork, 0, Quality.Small);
                CachedImage image;
                Optional<CachedImage> cache = CachedImage.getCache(identifier);
                if (cache.isPresent()) {
                    image = cache.get();
                } else {
                    image = CachedImage.createCache(identifier, path -> {
                        try {
                            OutputStream o = Files.newOutputStream(path);
                            var b = new BufferedInputStream(c.getInputStream());
                            byte[] buffer = new byte[10240];
                            int size;
                            while ((size = b.read(buffer)) != -1) {
                                o.write(buffer, 0, size);
                            }
                            o.close();
                            b.close();
                            return new Image(path.toFile().toURI().toURL().toString());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                    image.addToCache();
                }

                Platform.runLater(() -> {
                    bgTitleLbl.setText(bgSlideArtwork.getTitle());
                    bgAuthorLbl.setText(bgSlideArtwork.getUserName());

                    curImg = image;
                });
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        switchBackground();
                    }
                }, 20 * 1000);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        bgImg.fitWidthProperty().bind(root.widthProperty());
        bgImg.fitHeightProperty().bind(root.heightProperty());

        Main.getTpe().submit(() -> {
            loadLoginBg();
            switchBackground();
        });

//        final ChangeListener<Number> listener = (a, b, c) -> {
//            LOG.info("{} {}", root.getWidth(), root.getHeight());
//            bgImg.setViewport(new Rectangle2D(0, 0, root.getWidth(), root.getHeight()));
//        };
//
//        root.widthProperty().addListener(listener);
//        root.heightProperty().addListener(listener);
    }

    public void okButtonOnAction() {

    }
}
