package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.Closeable;
import xyz.zcraft.zpixiv.util.Refreshable;
import xyz.zcraft.zpixiv.util.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Stack;

import static xyz.zcraft.zpixiv.util.LayoutUtil.fillAnchor;

public class MainController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(MainController.class);
    private final Stack<Pair<Object, Parent>> contents = new Stack<>();
    public TextField topSearchBar;
    public AnchorPane contentPane;
    public AnchorPane main;
    public VBox closePageBtn;
    public VBox refreshBtn;
    public VBox msgPane;
    public ImageView profileImg;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetE = -1;
    private double offsetS = -1;

    public void profileBtnOnAction() {
        if(Main.getClient() != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Login.fxml"));
            closeAll();
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void exitBtnOnAction() {
        Platform.exit();
        Main.getTpe().shutdownNow();
        Main.getTimer().cancel();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing main layout...");

        Rectangle r = new Rectangle(profileImg.getFitWidth(), profileImg.getFitHeight());
        r.setArcWidth(profileImg.getFitWidth());
        r.setArcHeight(profileImg.getFitHeight());
        profileImg.setClip(r);

        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Demo.fxml"));
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void addContent(Object controller, Parent pane, boolean closePrevious) {
        if (closePrevious) closeAll();
        fillAnchor(pane);
        pane.setVisible(false);
        contentPane.getChildren().add(pane);
        final Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(pane.translateXProperty(), 100), new KeyValue(pane.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(350), new KeyValue(pane.translateXProperty(), 0, Interpolator.EASE_BOTH), new KeyValue(pane.opacityProperty(), 1))
        );
        fadeIn.playFromStart();
        pane.setVisible(true);
        contents.push(new Pair<>(controller, pane));
        checkPaneControlBtn();
        LOG.info(contents.toString());
    }

    public void closeAll() {
        for (int i = 0; i < Math.max(contents.size(), contentPane.getChildren().size()); i++) {
            closePageBtnOnAction();
        }
    }

    public synchronized void closePageBtnOnAction() {
        if (!contents.isEmpty()) {
            if (contents.peek().getKey() instanceof Closeable c) c.close();
            final var pane = contents.peek().getValue();
            final Timeline timeline = new Timeline(
                    new KeyFrame(Duration.millis(0), new KeyValue(pane.translateXProperty(), 0), new KeyValue(pane.opacityProperty(), 1)),
                    new KeyFrame(Duration.millis(200), new KeyValue(pane.translateXProperty(), -80, Interpolator.EASE_IN), new KeyValue(pane.opacityProperty(), 0))
            );
            timeline.setOnFinished(event -> {
                pane.setVisible(false);
                contentPane.getChildren().remove(pane);
                checkPaneControlBtn();
            });
            timeline.playFromStart();
            contents.pop();
        }

        LOG.info(contents.toString());
    }

    public void refreshBtnOnAction() {
        if (contents.peek() instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private void checkPaneControlBtn() {
        closePageBtn.setDisable(contents.isEmpty() && contentPane.getChildren().isEmpty());

        if (contents.isEmpty()) {
            refreshBtn.setDisable(true);
        } else {
            refreshBtn.setDisable(!(contents.peek() instanceof Refreshable));
        }
    }

    public void showAlert(String title, String msg) {
        final MsgController cont = MsgController.newInstance(title, msg);
        Platform.runLater(() -> {
            msgPane.getChildren().add(cont.getRoot());
            cont.playTimeLine();
        });
    }

    public void titleBarDragged(MouseEvent mouseEvent) {
        Main.getStage().setX(mouseEvent.getScreenX() - offsetX);
        Main.getStage().setY(mouseEvent.getScreenY() - offsetY);
    }

    public void titleBarPressed(MouseEvent mouseEvent) {
        offsetX = mouseEvent.getSceneX();
        offsetY = mouseEvent.getSceneY();
    }

    public void resizeE(MouseEvent mouseEvent) {
        if (Main.getStage().getWidth() + mouseEvent.getScreenX() - offsetE >= main.getMinWidth()) {
            Main.getStage().setWidth(Main.getStage().getWidth() + mouseEvent.getScreenX() - offsetE);
            offsetE = mouseEvent.getScreenX();
        }
    }

    public void resizeS(MouseEvent mouseEvent) {
        if (Main.getStage().getHeight() + mouseEvent.getSceneY() - offsetS >= main.getMinHeight()) {
            Main.getStage().setHeight(Main.getStage().getHeight() + mouseEvent.getSceneY() - offsetS);
            offsetS = mouseEvent.getSceneY();
        }
    }

    public void resizeSE(MouseEvent mouseEvent) {
        resizeS(mouseEvent);
        resizeE(mouseEvent);
    }

    public void resizeStart(MouseEvent mouseEvent) {
        offsetE = mouseEvent.getScreenX();
        offsetS = mouseEvent.getSceneY();
    }

    public void configBtnOnAction() {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Settings.fxml"));
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void demoBtnOnAction() {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Demo.fxml"));
            addContent(loader.getController(), loader.load(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
