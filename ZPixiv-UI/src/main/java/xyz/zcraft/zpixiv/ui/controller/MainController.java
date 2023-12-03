package xyz.zcraft.zpixiv.ui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    public HBox main;
    public Button closePageBtn;
    public Button refreshBtn;
    private LoginController loginController;

    public void profileBtnOnAction(KeyEvent keyEvent) {
    }

    public void menuBtnOnAction(ActionEvent actionEvent) {
    }

    public void discBtnOnAction(ActionEvent actionEvent) {
    }

    public void folBtnOnAction(ActionEvent actionEvent) {
    }

    public void setBtnOnAction(ActionEvent actionEvent) {
    }

    public void aboutBtnOnAction(ActionEvent actionEvent) {
    }

    public void minBtnOnAction(ActionEvent actionEvent) {

    }

    public void exitBtnOnAction(ActionEvent actionEvent) {
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

    public void closePageBtnOnAction(ActionEvent actionEvent) {
        if(controllers.isEmpty()) return;
        contentPane.getChildren().remove(contentPane.getChildren().size() - 1);
        controllers.pop();
        checkPaneControlBtn();
    }

    public void refreshBtnOnAction(ActionEvent actionEvent) {
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
}
