package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import org.controlsfx.control.PopOver;
import org.jetbrains.annotations.NotNull;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.api.user.PixivUser;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.*;

import java.io.IOException;
import java.net.URL;
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
    public Label userNameLbl;
    public AnchorPane topBar;
    public VBox sideBar;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetE = -1;
    private double offsetS = -1;
    private PopOver pop;

    public void profileBtnOnAction() {
        if (Main.getClient() != null) {
            pop.show(profileImg);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Login.fxml"));
                closeAll();
                addContent(loader.getController(), loader.load(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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

        contentPane.maxWidthProperty().bind(main.maxWidthProperty().subtract(sideBar.widthProperty()));
        contentPane.maxHeightProperty().bind(main.maxHeightProperty().subtract(topBar.heightProperty()));

        final Button exitLoginBtn = getExitLoginBtn();

        pop = new PopOver(profileImg);
        pop.setAnimated(true);
        pop.setCornerRadius(2);
        pop.setArrowIndent(0);
        pop.setContentNode(exitLoginBtn);
        pop.hide();

        try {
            final String s = Main.loadCookie();
            if (s != null) {
                profileImg.setDisable(true);
                userNameLbl.setText("登录中...");
                Main.getTpe().submit(() -> {
                    try {
                        SSLUtil.ignoreSsl();
                        final PixivClient client = new PixivClient(s, Main.getConfig().parseProxy(), true);
                        Main.setClient(client);
                        Platform.runLater(() -> Main.getMainController().userNameLbl.setText(client.getUserData().getName()));
                        final PixivUser userData = Main.getClient().getUserData();
                        final Identifier identifier = Identifier.of(userData.getId(), Identifier.Type.Profile, 0, Quality.Original);
                        final CachedImage image = LoadHelper.loadImage(userData.getProfileImg(), identifier, 1, null);
                        Platform.runLater(() -> {
                            profileImg.setImage(image.getImage());
                            profileImg.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            LOG.error("Failed to login", e);
                            profileImg.setDisable(false);
                            userNameLbl.setText("未登录");
                            Main.showAlert("错误", "自动登录失败");
                        });
                    }
                });
            }
        } catch (IOException e) {
            Main.showAlert("错误", "自动登录失败");
            LOG.error("Error logging in.", e);
        }
    }

    @NotNull
    private Button getExitLoginBtn() {
        final Button exitLoginBtn = new Button("退出登录");
        exitLoginBtn.setOnAction((e) -> {
            try {
                Main.deleteCookie();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Main.setClient(null);
            Main.getMainController().profileImg.setImage(new Image(ResourceLoader.loadAsStream("img/user.png")));
            Main.getMainController().userNameLbl.setText("未登录");
            Main.showAlert("提示", "已退出登录");
        });
        return exitLoginBtn;
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

    public void discBtnOnAction() {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Discovery.fxml"));
            final Parent load = loader.load();
            final DiscController controller = loader.getController();
            addContent(controller, load, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
