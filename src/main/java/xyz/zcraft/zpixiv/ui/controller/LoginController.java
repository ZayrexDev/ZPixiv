package xyz.zcraft.zpixiv.ui.controller;

import com.alibaba.fastjson2.JSONWriter;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.BgSlideArtwork;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.api.user.PixivUser;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.AnimationHelper;
import xyz.zcraft.zpixiv.util.CachedImage;
import xyz.zcraft.zpixiv.util.Closeable;
import xyz.zcraft.zpixiv.util.SSLUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.*;

public class LoginController implements Initializable, Closeable {
    private static final Logger LOG = LogManager.getLogger(LoginController.class);
    private static List<BgSlideArtwork> loginBgOrig;
    private final Timer timer = new Timer();
    private boolean closed = false;
    public TextArea cookieField;
    public ImageView bgAuthorImg;
    public ImageView bgImg;
    public Label bgTitleLbl;
    public Label bgAuthorLbl;
    public AnchorPane root;
    public CachedImage curImg;
    public VBox loadPane;
    public Button okBtn;
    public HBox loggedPane;
    public ImageView userImg;
    public Label userNameLbl;
    public VBox loginPane;

    private void setBackground() {
        if (curImg == null) return;
        Rectangle2D vp;
        final double w = root.getWidth();
        final double h = root.getHeight();
        final double imgW = curImg.getImage().getWidth();
        final double imgH = curImg.getImage().getHeight();
        if (imgW / imgH > w / h) {
            bgImg.setFitHeight(h);
            bgImg.setFitWidth(0);
            vp = new Rectangle2D((imgW - (imgH / h * w)) / 2, 0, imgW, imgH);
        } else {
            bgImg.setFitWidth(w);
            bgImg.setFitHeight(0);
            vp = new Rectangle2D(0, (imgH - imgW / w * h) / 2, imgW, imgH);
        }

        bgImg.setImage(curImg.getImage());
        bgImg.setViewport(vp);
    }

    private void loadLoginBg() {
        try {
            loginBgOrig = new PixivClient(null, Main.getConfig().parseProxy(), false).getLoginBackground();
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

                if (Main.getConfig().parseProxy() != null) {
                    c = url.openConnection(Main.getConfig().parseProxy());
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
                if (closed) return;
                Platform.runLater(() -> {
                    bgTitleLbl.setText(bgSlideArtwork.getTitle());
                    bgAuthorLbl.setText(bgSlideArtwork.getUserName());

                    curImg = image;
//                    setBackground();
                    bgImg.setImage(image.getImage());
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            switchBackground();
                        }
                    }, 20 * 1000);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
//        bgImg.fitWidthProperty().bind(((VBox) bgImg.getParent()).widthProperty().add(-1));
//        bgImg.fitHeightProperty().bind(((VBox) bgImg.getParent()).heightProperty().add(-1));

//        Main.getTpe().submit(() -> {
//            loadLoginBg();
//            switchBackground();
//        });

//        final ChangeListener<Number> listener = (a, b, c) -> {
//            LOG.info("{} {}", root.getWidth(), root.getHeight());
////            bgImg.setViewport(new Rectangle2D(0, 0, root.getWidth(), root.getHeight()));
////            setBackground();
//        };
//
//        root.widthProperty().addListener(listener);
//        root.heightProperty().addListener(listener);
    }

    public void okButtonOnAction() {
        AnimationHelper.getFadeInTransition(loadPane).playFromStart();
        loadPane.setVisible(true);
        okBtn.setDisable(true);

        Main.getTpe().submit(() -> {
            try {
                SSLUtil.ignoreSsl();
                final PixivClient client = new PixivClient(cookieField.getText(), Main.getConfig().parseProxy(), true);
                Main.setClient(client);
                Main.saveCookie(cookieField.getText());
                Platform.runLater(() -> Main.getMainController().userNameLbl.setText(client.getUserData().getName()));
                final PixivUser userData = client.getUserData();
                CachedImage image;
                Identifier identifier = Identifier.of(userData.getId(), Identifier.Type.Profile, 0, Quality.Original);
                Optional<CachedImage> cache = CachedImage.getCache(identifier);
                if (cache.isPresent()) {
                    image = cache.get();
                } else {
                    image = CachedImage.createCache(identifier, path -> {
                        try {
                            InputStream is;
                            URL url = new URL(userData.getProfileImg());
                            URLConnection c;

                            if (client.getProxy() != null)
                                c = url.openConnection(client.getProxy());
                            else c = url.openConnection();

                            c.setRequestProperty("Referer", "https://www.pixiv.net");

                            is = c.getInputStream();

                            return new Image(is);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    image.addToCache();
                }
                Platform.runLater(() -> {
                    userImg.setImage(image.getImage());
                    userNameLbl.setText(userData.getName());
                    Main.getMainController().profileImg.setImage(image.getImage());
                    AnimationHelper.getFadeOutTransition(loginPane).playFromStart();
                    AnimationHelper.getFadeInTransition(loggedPane).playFromStart();
                    loggedPane.setVisible(true);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void closeLogin() {
        Main.getMainController().closeAll();
    }

    @Override
    public void close() {
        closed = true;
    }
}
