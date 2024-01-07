package xyz.zcraft.zpixiv.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.*;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class IllustController implements Initializable {
    private static final Logger LOG = LogManager.getLogger();
    public Button bookmarkBtn;
    public Label pageLbl;
    public ImageView image;
    public Label loadLbl;
    public AnchorPane root;
    private PixivArtwork artwork;

    public void load(PixivArtwork artwork) {
        this.artwork = artwork;
        Platform.runLater(() -> {
            if (artwork.isBookmarked()) {
                bookmarkBtn.setStyle("-fx-shape: \"" + SvgIcon.FULL + "\"; -fx-background-color: #ff4563;");
            } else bookmarkBtn.setStyle("");
            if (artwork.getOrigData().getPageCount() >= 2) {
                pageLbl.setText(String.valueOf(artwork.getOrigData().getPageCount()));
                pageLbl.setVisible(true);
            }
        });
        Main.getTpe().submit(() -> {
            final Identifier identifier = Identifier.of(artwork.getOrigData().getId(), Identifier.Type.Artwork, 0, Quality.Small);
            final String urlString = Objects.requireNonNullElse(
                    artwork.getOrigData().getUrls().getString(Quality.Small.getStr()),
                    artwork.getOrigData().getUrls().getString("250x250")
            );
            final CachedImage cachedImage = LoadHelper.loadImage(urlString, identifier, 1, null);
            Platform.runLater(() -> {
                image.setImage(cachedImage.getImage());
                AnimationHelper.getFadeOutTransition(loadLbl).playFromStart();
            });
        });
    }

    public void bookmarkBtnOnAction() {
        if (!artwork.isBookmarked()) {
            bookmarkBtn.setDisable(true);
            Main.getTpe().submit(() -> {
                try {
                    if (!Main.getClient().addBookmark(artwork)) throw new RuntimeException();
                    Main.showAlert("提示", "收藏成功~");
                } catch (Exception e) {
                    Main.showAlert("错误", "收藏失败");
                    LOG.error("Error bookmarking artwork", e);
                }

                bookmarkBtn.setDisable(false);
                if (artwork.isBookmarked()) {
                    bookmarkBtn.setStyle("-fx-shape: \"" + SvgIcon.FULL + "\"; -fx-background-color: #ff4563;");
                } else bookmarkBtn.setStyle("");
            });
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void openArtwork() {
        root.setDisable(true);
        try {
            final FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Artwork.fxml"));
            final Parent p;
            p = loader.load();
            final ArtworkController controller = loader.getController();
            Main.getTpe().submit(() -> {
                controller.load(Main.getClient(), artwork);
                Platform.runLater(() -> {
                    Main.getMainController().addContent(controller, p, false);
                    root.setDisable(false);
                });
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
