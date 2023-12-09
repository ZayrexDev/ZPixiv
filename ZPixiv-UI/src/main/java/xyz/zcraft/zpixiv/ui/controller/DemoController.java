package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;
import xyz.zcraft.zpixiv.ui.util.SSLUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class DemoController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(DemoController.class);
    private final FadeTransition pbHide = new FadeTransition();
    private final FadeTransition pbShow = new FadeTransition();
    public TextField cookieField;
    public TextField proxyHostField;
    public TextField proxyPortField;
    public TextField idField;
    public TextField msgTitleField;
    public TextField msgContentField;
    public Label userNameLbl;
    public Button openArtworkBtn;
    public ProgressBar progressBar;
    public Button loginBtn;
    private PixivClient client;

    public void openArtworkBtnOnAction() {
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
                    controller.load(client, client.getArtwork(idField.getText()));
                    Platform.runLater(() -> {
                        Main.getMainController().addContent(controller, p);
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
        final Path conf = Path.of("cache.conf");
        StringBuilder sb = new StringBuilder();
        sb.append("cookie=").append(cookieField.getText().split("=")[1]).append(";");
        sb.append("proxyHost=").append(proxyHostField.getText()).append(";");
        sb.append("proxyPort=").append(proxyPortField.getText()).append(";");
        sb.append("id=").append(idField.getText()).append(";");
        try {
            Files.writeString(conf, sb.toString());
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    public void read() {
        final Path conf = Path.of("cache.conf");
        if (Files.exists(conf)) {
            final String s;
            try {
                s = Files.readString(conf);
                final String[] split = s.split(";");
                for (String string : split) {
                    final String[] split1 = string.split("=");
                    switch (split1[0]) {
                        case "cookie" -> cookieField.setText("PHPSESSID=" + split1[1]);
                        case "proxyHost" -> proxyHostField.setText(split1[1]);
                        case "proxyPort" -> proxyPortField.setText(split1[1]);
                        case "id" -> idField.setText(split1[1]);
                    }
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public void login() {
        LOG.info("Logging with cookie {}, proxy {}",
                cookieField.getText(),
                proxyHostField.getText() + ":" + proxyPortField.getText()
        );
        showPb();
        loginBtn.setDisable(true);
        try {
            Main.getTpe().submit(() -> {
                try {
                    Proxy proxy;
                    if (proxyHostField.getText() == null || proxyPortField.getText() == null || proxyHostField.getText().isBlank()) {
                        proxy = null;
                    } else {
                        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostField.getText(), Integer.parseInt(proxyPortField.getText())));
                    }
                    this.client = new PixivClient(cookieField.getText(), proxy, true);
                    Platform.runLater(() -> {
                        userNameLbl.setText("已登录: " + client.getUserData().getName());
                        openArtworkBtn.setDisable(false);
                        hidePb();
                        loginBtn.setDisable(false);
                    });
                } catch (Exception e) {
                    Main.showAlert("错误", "登录失败");
                    LOG.error("Error logging in", e);
                    Platform.runLater(() -> {
                        hidePb();
                        loginBtn.setDisable(false);
                    });
                }
            });
        } catch (Exception e) {
            Main.showAlert("错误", "登录失败");
            LOG.error("Error logging in", e);
            Platform.runLater(() -> {
                hidePb();
                loginBtn.setDisable(false);
            });
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
