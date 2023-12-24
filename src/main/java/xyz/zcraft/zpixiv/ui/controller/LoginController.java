package xyz.zcraft.zpixiv.ui.controller;

import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.BgSlideArtwork;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class LoginController implements Initializable {
    public TextArea cookieField;
    public ImageView bgAuthorImg;
    public ImageView bgImg;
    public Label bgTitleLbl;
    public Label bgAuthorLbl;
    private final List<BgSlideArtwork> loginBackground = new LinkedList<>();

    private void switchBackground() {
        BgSlideArtwork cur = loginBackground.get(new Random().nextInt(loginBackground.size()));
        cur.getUrl().getString("medium");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            PixivClient c = new PixivClient(null, null, false);
            loginBackground.addAll(c.getLoginBackground());
            switchBackground();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void okButtonOnAction() {

    }
}
