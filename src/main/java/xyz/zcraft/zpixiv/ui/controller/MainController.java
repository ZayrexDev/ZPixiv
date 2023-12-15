package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.ui.util.Refreshable;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.Stack;

import static xyz.zcraft.zpixiv.ui.util.LayoutUtil.fillAnchor;

public class MainController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(MainController.class);
    private final Stack<Object> controllers = new Stack<>();
    public TextField topSearchBar;
    public AnchorPane contentPane;
    public AnchorPane main;
    public VBox closePageBtn;
    public VBox refreshBtn;
    public VBox msgPane;
    private double offsetX = 0;
    private double offsetY = 0;
    private double offsetE = -1;
    private double offsetS = -1;

    public void profileBtnOnAction() {
    }

    public void exitBtnOnAction() {
        Platform.exit();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing main layout...");
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Demo.fxml"));
            addContent(loader.getController(), loader.load());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addContent(Object controller, Node pane) {
        fillAnchor(pane);
        controllers.push(controller);
        pane.setVisible(false);
        contentPane.getChildren().add(pane);
        final Timeline fadein = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(pane.translateXProperty(), 100), new KeyValue(pane.opacityProperty(), 0)),
                new KeyFrame(Duration.millis(500), new KeyValue(pane.translateXProperty(), 0, Interpolator.EASE_OUT), new KeyValue(pane.opacityProperty(), 1))
        );
        fadein.playFromStart();
        pane.setVisible(true);
        checkPaneControlBtn();
    }


    public void closePageBtnOnAction() {
        if (controllers.isEmpty()) return;
        final var pane = contentPane.getChildren().get(contentPane.getChildren().size() - 1);
        final Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(pane.translateXProperty(), 0), new KeyValue(pane.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(200), new KeyValue(pane.translateXProperty(), -80, Interpolator.EASE_IN), new KeyValue(pane.opacityProperty(), 0))
        );
        timeline.setOnFinished(event -> {
            pane.setVisible(false);
            contentPane.getChildren().remove(pane);
            controllers.pop();
            checkPaneControlBtn();
        });
        timeline.playFromStart();
    }

    public void refreshBtnOnAction() {
        if (controllers.peek() instanceof Refreshable refreshable) {
            refreshable.refresh();
        }
    }

    private void checkPaneControlBtn() {
        if (!controllers.isEmpty()) {
            refreshBtn.setDisable(!(controllers.peek() instanceof Refreshable));
            closePageBtn.setDisable(false);
        } else {
            refreshBtn.setDisable(true);
            closePageBtn.setDisable(true);
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
}