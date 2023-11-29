package xyz.zcraft.zpixiv.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
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
    public WebView descView;

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

    public void load(PixivClient client, PixivArtwork artwork) {
        titleLbl.setText(artwork.getTitle());

        if(artwork.getXRestrict() == 1) {
            xResLbl.setVisible(true);
        } else {
            titleBox.getChildren().remove(0);
        }

        if(artwork.getDescription().trim().isEmpty()) {
            ((VBox) descView.getParent()).getChildren().remove(descView);
        } else {
            descView.setContextMenuEnabled(false);
            descView.getEngine().setUserStyleSheetLocation("data:,body{font: 12px Arial;}");
            descView.getEngine().loadContent(artwork.getDescription());
        }

        authorNameLbl.setText(artwork.getAuthor().getName());
        pubDateLbl.setText(artwork.getCreateDate());

        praiseLbl.setText(String.valueOf(artwork.getLikeCount()));
        likesLbl.setText(String.valueOf(artwork.getBookmarkCount()));

        try {
            final String profileImg = artwork.getAuthor().getProfileImg();
            authorImg.setImage(getImg(profileImg, client.getProxy()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            client.getFullPages(artwork);
            img.setImage(getImg(artwork.getImageUrls().get(0), client.getProxy()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO
    private Image getImg(String imgUrl, Proxy proxy) throws IOException {
        InputStream is;
        URL url = new URL(imgUrl);
        URLConnection c = url.openConnection(proxy);

        c.setRequestProperty("Referer", "https://www.pixiv.net");

        is = c.getInputStream();

        return new Image(is);
    }
}
