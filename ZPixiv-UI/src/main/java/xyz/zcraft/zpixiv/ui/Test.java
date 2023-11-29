package xyz.zcraft.zpixiv.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.user.LoginSession;
import xyz.zcraft.zpixiv.ui.controller.ArtworkController;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class Test extends Application {
    private static final String COOKIE = "";
    private static final String ID = "";
    private static final Proxy PROXY = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(0));

    @Override
    public void start(Stage stage) throws Exception {
        final URL url = ResourceLoader.load("fxml/Artwork.fxml");
        final FXMLLoader loader = new FXMLLoader(url);

        stage.setScene(new Scene(loader.load()));

        final ArtworkController controller = loader.getController();

        PixivClient client = new PixivClient(COOKIE, PROXY);

        controller.load(client, client.getArtwork(ID));

        stage.show();
    }
}
