package xyz.zcraft.zpixiv.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import static xyz.zcraft.zpixiv.ui.util.LayoutUtil.fillAnchor;

public class MainController implements Initializable {
    private static final Logger LOG = LogManager.getLogger(MainController.class);
    public TextField topSearchBar;
    public AnchorPane contentPane;
    public HBox main;
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
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        LOG.info("Initializing main layout...");
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/login.fxml"));
            contentPane.getChildren().add(fillAnchor(loader.load()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
