package xyz.zcraft.zpixiv.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
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
    public Button closePageBtn;
    public Button refreshBtn;
    public VBox msgPane;

    public void profileBtnOnAction() {
    }

    public void menuBtnOnAction() {
    }

    public void discBtnOnAction() {
    }

    public void folBtnOnAction() {
    }

    public void setBtnOnAction() {
    }

    public void aboutBtnOnAction() {
    }

    public void minBtnOnAction() {

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
        contentPane.getChildren().add(pane);
        checkPaneControlBtn();
    }

    public void closePageBtnOnAction() {
        if (controllers.isEmpty()) return;
        contentPane.getChildren().remove(contentPane.getChildren().size() - 1);
        controllers.pop();
        checkPaneControlBtn();
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

    private double offsetX = 0;
    private double offsetY = 0;
    public void titleBarPressed(MouseEvent mouseEvent) {
        offsetX = mouseEvent.getSceneX();
        offsetY = mouseEvent.getSceneY();
    }

    public void resizeE(MouseEvent mouseEvent) {
        if(Main.getStage().getWidth() + mouseEvent.getScreenX() - offsetE >= main.getMinWidth()) {
            Main.getStage().setWidth(Main.getStage().getWidth() + mouseEvent.getScreenX() - offsetE);
            offsetE = mouseEvent.getScreenX();
        }
    }

    public void resizeS(MouseEvent mouseEvent) {
        if(Main.getStage().getHeight() + mouseEvent.getSceneY() - offsetS >= main.getMinHeight()) {
            Main.getStage().setHeight(Main.getStage().getHeight() + mouseEvent.getSceneY() - offsetS);
            offsetS = mouseEvent.getSceneY();
        }
    }

    public void resizeSE(MouseEvent mouseEvent) {
        resizeS(mouseEvent);
        resizeE(mouseEvent);
    }

    private double offsetE = -1;
    private double offsetS = -1;

    public void resizeStart(MouseEvent mouseEvent) {
        offsetE = mouseEvent.getScreenX();
        offsetS = mouseEvent.getSceneY();
    }
}
