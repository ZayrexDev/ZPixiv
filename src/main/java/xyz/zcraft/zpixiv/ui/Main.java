package xyz.zcraft.zpixiv.ui;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.ui.controller.InspectController;
import xyz.zcraft.zpixiv.ui.controller.MainController;
import xyz.zcraft.zpixiv.util.CachedImage;
import xyz.zcraft.zpixiv.util.Config;
import xyz.zcraft.zpixiv.util.ResourceLoader;
import xyz.zcraft.zpixiv.util.SSLUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main extends Application {
    @Getter
    public static final LinkedList<CachedImage> loginBackground = new LinkedList<>();
    private static final Logger LOG = LogManager.getLogger(Main.class);
    @Getter
    private static final ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    @Getter
    private static final Timer timer = new Timer();
    private static final Path configPath = Path.of("config.json");
    @Getter
    private static Config config;
    @Getter
    private static Stage stage = null;
    @Getter
    private static MainController mainController = null;
    @Getter @Setter
    private static PixivClient client = null;

    public static void main(String[] args) {
        try {
            config = loadConfig();
        } catch (Exception e) {
            config = new Config();
            LOG.error("Error loading config.", e);
        }
        launch(args);
    }

    public static void showAlert(String title, String content) {
        mainController.showAlert(title, content);
    }

    public static void saveConfig() throws Exception {
        String string = JSONObject.from(config).toString(JSONWriter.Feature.PrettyFormat);
        Files.writeString(configPath, string);
    }

    public static Config loadConfig() throws Exception {
        if (Files.exists(configPath)) {
            return JSONObject.parseObject(Files.readString(configPath)).to(Config.class);
        } else {
            return new Config();
        }
    }

    public static void openInspectStage(CachedImage[] images) {
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(ResourceLoader.load("fxml/Inspect.fxml"));

            Stage inspectStage = new Stage();
            inspectStage.setScene(new Scene(fxmlLoader.load()));
            InspectController controller = fxmlLoader.getController();
            controller.init(inspectStage, images);

            inspectStage.show();
        } catch (IOException e) {
            LOG.error("Failed to open inspection stage.", e);
            showAlert("错误", "打开失败");
        }
    }

    @Override
    public void start(Stage stage) {
        Main.stage = stage;
        try {
            SSLUtil.ignoreSsl();
            LOG.info("Loading frame");
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("fxml/Main.fxml"));
            final Pane main = loader.load();
            main.maxWidthProperty().bind(stage.widthProperty());
            main.maxHeightProperty().bind(stage.heightProperty());
            mainController = loader.getController();
            Scene s = new Scene(main);

            stage.setScene(s);

            stage.initStyle(StageStyle.UNDECORATED);
            stage.show();
        } catch (IOException e) {
            LOG.error("Exception in window initialize", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
