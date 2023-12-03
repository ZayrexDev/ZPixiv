package xyz.zcraft.zpixiv.ui.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;
import xyz.zcraft.zpixiv.ui.util.SSLUtil;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class DemoController {
    public TextField cookieField;
    public TextField proxyHostField;
    public TextField proxyPortField;
    public TextField idField;

    public void openArtworkBtnOnAction() {
        try {
            SSLUtil.ignoreSsl();

            PixivClient client = new PixivClient(
                    cookieField.getText(),
                    new Proxy(Proxy.Type.HTTP,
                            new InetSocketAddress(proxyHostField.getText(), Integer.parseInt(proxyPortField.getText()))),
                    false
            );

            final URL url = ResourceLoader.load("fxml/Artwork.fxml");
            final FXMLLoader loader = new FXMLLoader(url);

            Parent p = loader.load();

            final ArtworkController controller = loader.getController();

            controller.load(client, client.getArtwork(idField.getText()));
            Main.getMainController().addContent(controller, p);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
