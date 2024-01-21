package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.ui.container.AnimatedFlowPane;
import xyz.zcraft.zpixiv.util.AnimationHelper;
import xyz.zcraft.zpixiv.util.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DiscController implements Initializable {
    private static final Logger LOG = LogManager.getLogger();
    public Region refreshBtn;
    public ComboBox<PixivClient.Mode> modeCombo;
    public AnimatedFlowPane artworkPane;
    public HBox topBar;
    private TranslateTransition topBarAnimation;
    private RotateTransition refreshBtnRotateAnim;

    public void refresh() {
        final FadeTransition fadeOutTransition = AnimationHelper.getFadeOutTransition(artworkPane);
        fadeOutTransition.setOnFinished((e) -> artworkPane.getChildren().clear());
        fadeOutTransition.playFromStart();

        refreshBtn.setDisable(true);
        refreshBtnRotateAnim.playFromStart();
        Main.getTpe().submit(() -> {
            try {
                final List<PixivArtwork> discovery = Main.getClient().getDiscovery(modeCombo.getValue(), 50);
                for (PixivArtwork pixivArtwork : discovery) {
                    Platform.runLater(() -> {
                        try {
                            final FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Illust.fxml"));
                            final Parent p = loader.load();
                            final IllustController controller = loader.getController();
                            controller.load(pixivArtwork);
                            artworkPane.getChildren().add(p);
                        } catch (IOException e) {
                            Main.showAlert("错误", "获取作品时出现问题");
                            LOG.error("Failed to get artwork.", e);
                        }
                    });
                }
                Platform.runLater(() -> {
                    refreshBtnRotateAnim.stop();
                    refreshBtn.setRotate(0);
                    refreshBtn.setDisable(false);
                    AnimationHelper.getFadeInTransition(artworkPane).playFromStart();
                });
            } catch (IOException e) {
                Main.showAlert("错误", "无法打开发现");
                LOG.error("Failed to open discovery.", e);
                Platform.runLater(() -> refreshBtn.setDisable(false));
            }
        });
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        modeCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(PixivClient.Mode object) {
                return switch (object) {
                    case ALL -> "全部";
                    case SAFE -> "普通";
                    case R18 -> "涩涩";
                };
            }

            @Override
            public PixivClient.Mode fromString(String string) {
                return null;
            }
        });
        modeCombo.getItems().add(PixivClient.Mode.SAFE);
        if (Main.getClient().getUserData().getXRestrict() != 0) {
            modeCombo.getItems().addAll(PixivClient.Mode.R18, PixivClient.Mode.ALL);
            modeCombo.setVisible(true);
        } else {
            modeCombo.setVisible(false);
        }
        modeCombo.getSelectionModel().select(PixivClient.Mode.SAFE);

        refreshBtnRotateAnim = new RotateTransition(Duration.seconds(1), refreshBtn);
        refreshBtnRotateAnim.setCycleCount(Animation.INDEFINITE);
        refreshBtnRotateAnim.setFromAngle(0);
        refreshBtnRotateAnim.setToAngle(-360);

        artworkPane.setOnScroll(event -> {
            if (event.getDeltaY() < 0) {
                if (topBarAnimation != null) topBarAnimation.stop();
                topBarAnimation = new TranslateTransition();
                topBarAnimation.setNode(topBar);
                topBarAnimation.setDuration(Duration.millis(300));
                topBarAnimation.setFromY(topBar.getTranslateY());
                topBarAnimation.setToY(-80);
                topBarAnimation.setInterpolator(Interpolator.EASE_BOTH);
                topBarAnimation.playFromStart();
            } else if (event.getDeltaY() > 0) {
                if (topBarAnimation != null) topBarAnimation.stop();
                topBarAnimation = new TranslateTransition();
                topBarAnimation.setNode(topBar);
                topBarAnimation.setDuration(Duration.millis(300));
                topBarAnimation.setFromY(topBar.getTranslateY());
                topBarAnimation.setToY(0);
                topBarAnimation.setInterpolator(Interpolator.EASE_BOTH);
                topBarAnimation.playFromStart();
            }
        });

        refresh();
    }
}
