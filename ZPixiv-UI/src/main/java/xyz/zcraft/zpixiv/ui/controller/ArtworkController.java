package xyz.zcraft.zpixiv.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;

import java.net.URL;
import java.util.ResourceBundle;

public class ArtworkController implements Initializable {
    public ImageView img;
    public Button nextPageBtn;
    public Button prevPageBtn;
    public Label titleLbl;
    public TextArea descLbl;
    public ImageView authorImg;
    public Label authorNameLbl;
    public Label pubDateLbl;
    public Button followBtn;
    public Label praiseLbl;
    public Label likesLbl;
    public Label viewLbl;
    public Button praiseBtn;
    public Button likeBtn;
    public Button hidLikeBtn;
    public Label xResLbl;
    public HBox titleBox;

    public void nextPageBtnOnAction(ActionEvent actionEvent) {
    }

    public void prevPageBtnOnAction(ActionEvent actionEvent) {
    }

    public void followBtnOnAction(ActionEvent actionEvent) {
    }

    public void praiseBtnOnAction(ActionEvent actionEvent) {
    }

    public void hidLikeBtnOnAction(ActionEvent actionEvent) {
    }

    public void likeBtnOnAction(ActionEvent actionEvent) {
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void load(PixivArtwork artwork) {
        titleLbl.setText(artwork.getTitle());

        if(artwork.getXRestrict() == 1) {
            xResLbl.setVisible(true);
        } else {
            titleBox.getChildren().remove(0);
        }

        descLbl.setText(artwork.getDescription());

        authorNameLbl.setText(artwork.getAuthor().getName());
        pubDateLbl.setText(artwork.getCreateDate());

        praiseLbl.setText(String.valueOf(artwork.getLikeCount()));
        likesLbl.setText(String.valueOf(artwork.getBookmarkCount()));
    }
}
