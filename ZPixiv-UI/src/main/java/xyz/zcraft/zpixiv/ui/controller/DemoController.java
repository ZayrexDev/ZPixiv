package xyz.zcraft.zpixiv.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
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

public class DemoController {
    private static final Logger LOG = LogManager.getLogger(DemoController.class);
    public TextField cookieField;
    public TextField proxyHostField;
    public TextField proxyPortField;
    public TextField idField;
    public TextField msgTitleField;
    public TextField msgContentField;

    public void openArtworkBtnOnAction() {
        try {
            SSLUtil.ignoreSsl();

            Proxy proxy;

            if (proxyHostField.getText() == null || proxyPortField.getText() == null || proxyHostField.getText().isBlank()) {
                proxy = null;
            } else {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostField.getText(), Integer.parseInt(proxyPortField.getText())));
            }

            PixivClient client = new PixivClient(cookieField.getText(), proxy, false);

            final URL url = ResourceLoader.load("fxml/Artwork.fxml");
            final FXMLLoader loader = new FXMLLoader(url);

            Parent p = loader.load();

            final ArtworkController controller = loader.getController();

            controller.load(client, client.getArtwork(idField.getText()));
            Main.getMainController().addContent(controller, p);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    public void showMsgBtnOnAction() {
        Main.getMainController().showAlert(msgTitleField.getText(), msgContentField.getText());
    }

    public void save(ActionEvent actionEvent) {
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

    public void read(ActionEvent actionEvent) {
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
}
