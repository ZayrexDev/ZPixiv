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

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main extends Application {
    private static final Logger LOG = LogManager.getLogger(Main.class);

    @Getter
    private static Stage stage = null;

    @Getter
    private static MainController mainController = null;

    @Getter
    private static final ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    @Override
    public void start(Stage stage) {
        Main.stage = stage;
        try {
            LOG.info("Loading frame");
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("fxml/Main.fxml"));
            final Parent main = loader.load();
            mainController = loader.getController();
            Scene s = new Scene(main);

            stage.setScene(s);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.show();
        } catch (IOException e) {
            LOG.error("Exception in window initialize", e);
        }
    }

    public static void showAlert(String title, String content) {
        mainController.showAlert(title, content);
    }
}
