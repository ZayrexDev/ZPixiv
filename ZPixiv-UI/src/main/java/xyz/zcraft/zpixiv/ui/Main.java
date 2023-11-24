package xyz.zcraft.zpixiv.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.io.IOException;

public class Main extends Application {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Main.fxml"));
            final Parent main = loader.load();
            Scene s = new Scene(main);
            stage.setScene(s);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
