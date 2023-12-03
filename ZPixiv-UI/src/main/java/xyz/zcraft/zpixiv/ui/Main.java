package xyz.zcraft.zpixiv.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.ui.controller.MainController;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.io.IOException;

public class Main extends Application {
    private static final Logger LOG = LogManager.getLogger(Main.class);
    private static Stage stage = null;

    @Getter
    private static MainController mainController = null;

    @Override
    public void start(Stage stage) {
        Main.stage = stage;
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Main.fxml"));
            final Parent main = loader.load();
            mainController = loader.getController();
            Scene s = new Scene(main);
            stage.setScene(s);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
