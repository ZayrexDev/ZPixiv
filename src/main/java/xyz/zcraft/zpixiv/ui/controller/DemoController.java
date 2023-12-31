package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.ResourceLoader;
import xyz.zcraft.zpixiv.util.SSLUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.ResourceBundle;

public class DemoController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(DemoController.class);
    private final FadeTransition pbHide = new FadeTransition();
    private final FadeTransition pbShow = new FadeTransition();
    public TextField idField;
    public TextField msgTitleField;
    public TextField msgContentField;
    public Button openArtworkBtn;
    public ProgressBar progressBar;
    private final Path configPath = Main.getDataPath().resolve("config.prop");

    public void openArtworkBtnOnAction() {
        if (Main.getClient() == null) {
            Main.showAlert("提示", "请先登录");
            return;
        }
        try {
            showPb();
            openArtworkBtn.setDisable(true);
            SSLUtil.ignoreSsl();

            final URL url = ResourceLoader.load("fxml/Artwork.fxml");
            final FXMLLoader loader = new FXMLLoader(url);
            final Parent p = loader.load();
            final ArtworkController controller = loader.getController();
            Main.getTpe().submit(() -> {
                try {
                    controller.load(Main.getClient(), Main.getClient().getArtwork(idField.getText()));
                    Platform.runLater(() -> {
                        Main.getMainController().addContent(controller, p, false);
                        hidePb();
                        openArtworkBtn.setDisable(false);
                    });
                } catch (IOException e) {
                    Main.showAlert("错误", "无法打开作品");
                    LOG.error("Error opening artwork", e);
                    Platform.runLater(() -> {
                        hidePb();
                        openArtworkBtn.setDisable(false);
                    });
                }
            });
        } catch (Exception e) {
            Main.showAlert("错误", "无法打开作品");
            LOG.error("Error opening artwork", e);
        }
    }

    public void showMsgBtnOnAction() {
        Main.getMainController().showAlert(msgTitleField.getText(), msgContentField.getText());
    }

    public void save() {
        try {
            Properties properties = new Properties();
            properties.put("id", idField.getText());
            properties.store(Files.newOutputStream(configPath), "ZPixiv Demo");
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public void read() {
        if (!Files.exists(configPath)) return;
        try {
            final Properties properties = new Properties();
            properties.load(Files.newInputStream(configPath));
            idField.setText(properties.getProperty("id"));
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private void showPb() {
        pbHide.stop();
        pbShow.playFromStart();
        progressBar.setVisible(true);
    }

    private void hidePb() {
        pbShow.stop();
        pbHide.playFromStart();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pbHide.setNode(progressBar);
        pbHide.setDuration(Duration.millis(200));
        pbHide.setOnFinished((u) -> progressBar.setVisible(false));
        pbHide.setFromValue(1.0);
        pbHide.setToValue(0.0);

        pbShow.setNode(progressBar);
        pbShow.setDuration(Duration.millis(200));
        pbShow.setFromValue(0.0);
        pbShow.setToValue(1.0);
    }
}
