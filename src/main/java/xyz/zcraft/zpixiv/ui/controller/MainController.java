package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.api.user.PixivUser;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Stack;

import static xyz.zcraft.zpixiv.util.LayoutUtil.fillAnchor;

public class MainController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(MainController.class);
    private final Stack<Pair<Object, Parent>> contents = new Stack<>();
    public TextField topSearchBar;
    public AnchorPane contentPane;
    public AnchorPane main;
    public VBox closePageBtn;
    public VBox refreshBtn;
    public VBox msgPane;
    public ImageView profileImg;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetE = -1;
    private double offsetS = -1;

    public void profileBtnOnAction() {
        if (Main.getClient() != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Login.fxml"));
            closeAll();
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exitBtnOnAction() {
        Platform.exit();
        Main.getTpe().shutdownNow();
        Main.getTimer().cancel();
    }

    @Override
    public void initialize(URL urlIgnored, ResourceBundle resourceBundle) {
        LOG.info("Initializing main layout...");

        Rectangle r = new Rectangle(profileImg.getFitWidth(), profileImg.getFitHeight());
        r.setArcWidth(profileImg.getFitWidth());
        r.setArcHeight(profileImg.getFitHeight());
        profileImg.setClip(r);

        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Demo.fxml"));
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            final String s = Main.loadCookie();
            if (s != null) {
                profileImg.setDisable(true);

                Main.getTpe().submit(() -> {
                    try {
                        SSLUtil.ignoreSsl();
                        Main.setClient(new PixivClient(s, Main.getConfig().parseProxy(), true));
                        final PixivUser userData = Main.getClient().getUserData();
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

                                    if (Main.getClient().getProxy() != null)
                                        c = url.openConnection(Main.getClient().getProxy());
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
                        Platform.runLater(() -> profileImg.setImage(image.getImage()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (IOException e) {
            Main.showAlert("错误", "自动登录失败");
            LOG.error("Error logging in.", e);
        }
    }

    public synchronized void addContent(Object controller, Parent pane, boolean closePrevious) {
        if (closePrevious) closeAll();
        fillAnchor(pane);
        pane.setVisible(false);
        contentPane.getChildren().add(pane);
        final Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(pane.translateXProperty(), 100), new KeyValue(pane.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(pane.translateXProperty(), 0, Interpolator.EASE_OUT), new KeyValue(pane.opacityProperty(), 1))
        );
        fadeIn.playFromStart();
        pane.setVisible(true);
        contents.push(new Pair<>(controller, pane));
        checkPaneControlBtn();
    }

    public void closeAll() {
        for (int i = 0; i < contents.size(); i++) {
            closePageBtnOnAction();
        }
    }

    public synchronized void closePageBtnOnAction() {
        if (!contents.isEmpty()) {
            if (contents.peek().getKey() instanceof Closeable c) c.close();
            final var pane = contents.peek().getValue();
            final Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(0), new KeyValue(pane.translateXProperty(), 0), new KeyValue(pane.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(200), new KeyValue(pane.translateXProperty(), -100, Interpolator.EASE_IN), new KeyValue(pane.opacityProperty(), 0))
            );
            timeline.setOnFinished(event -> {
                pane.setVisible(false);
                contentPane.getChildren().remove(pane);
                checkPaneControlBtn();
            });
            timeline.playFromStart();
            contents.pop();
        }
    }

    public void refreshBtnOnAction() {
        if (contents.peek() instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private void checkPaneControlBtn() {
        closePageBtn.setDisable(contents.isEmpty() && contentPane.getChildren().isEmpty());

        if (contents.isEmpty()) {
            refreshBtn.setDisable(true);
        } else {
            refreshBtn.setDisable(!(contents.peek() instanceof Refreshable));
        }
    }

    public void showAlert(String title, String msg) {
        final MsgController cont = MsgController.newInstance(title, msg);
        Platform.runLater(() -> {
            msgPane.getChildren().add(cont.getRoot());
            cont.playTimeLine();
        });
    }

    public void titleBarDragged(MouseEvent mouseEvent) {
        Main.getStage().setX(mouseEvent.getScreenX() - offsetX);
        Main.getStage().setY(mouseEvent.getScreenY() - offsetY);
    }

    public void titleBarPressed(MouseEvent mouseEvent) {
        offsetX = mouseEvent.getSceneX();
        offsetY = mouseEvent.getSceneY();
    }

    public void resizeE(MouseEvent mouseEvent) {
        if (Main.getStage().getWidth() + mouseEvent.getScreenX() - offsetE >= main.getMinWidth()) {
            Main.getStage().setWidth(Main.getStage().getWidth() + mouseEvent.getScreenX() - offsetE);
            offsetE = mouseEvent.getScreenX();
        }
    }

    public void resizeS(MouseEvent mouseEvent) {
        if (Main.getStage().getHeight() + mouseEvent.getSceneY() - offsetS >= main.getMinHeight()) {
            Main.getStage().setHeight(Main.getStage().getHeight() + mouseEvent.getSceneY() - offsetS);
            offsetS = mouseEvent.getSceneY();
        }
    }

    public void resizeSE(MouseEvent mouseEvent) {
        resizeS(mouseEvent);
        resizeE(mouseEvent);
    }

    public void resizeStart(MouseEvent mouseEvent) {
        offsetE = mouseEvent.getScreenX();
        offsetS = mouseEvent.getSceneY();
    }

    public void configBtnOnAction() {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Settings.fxml"));
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void demoBtnOnAction() {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Demo.fxml"));
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void minimizeBtnOnAction() {
        final var oProperty = Main.getStage().opacityProperty();

        final Timeline timeline = new Timeline();
        timeline.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(0), new KeyValue(oProperty, 1)),
                new KeyFrame(Duration.millis(100), new KeyValue(oProperty, 0))
        );
        timeline.setOnFinished((e) -> {
            Main.getStage().setIconified(true);
            Main.getStage().setOpacity(1);
        });
        timeline.playFromStart();
    }
}
