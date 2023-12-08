package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.ui.util.Refreshable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;

public class ArtworkController implements Initializable, Refreshable {
    private static final Logger LOG = LogManager.getLogger(ArtworkController.class);
    @FXML
    public ImageView imgView;
    @FXML
    public Button nextPageBtn;
    @FXML
    public Button prevPageBtn;
    @FXML
    public Label titleLbl;
    @FXML
    public ImageView authorImg;
    @FXML
    public Label authorNameLbl;
    @FXML
    public Label pubDateLbl;
    @FXML
    public Button followBtn;
    @FXML
    public Label likeLbl;
    @FXML
    public Label bookmarkLbl;
    @FXML
    public Label viewLbl;
    @FXML
    public Region likeBtn;
    @FXML
    public Region bookmarkBtn;
    @FXML
    public Label xResLbl;
    @FXML
    public HBox titleBox;
    @FXML
    public WebView descView;
    @FXML
    public VBox root;
    @FXML
    public ProgressIndicator loadProgress;
    @FXML
    public AnchorPane imgAnchor;
    @FXML
    public ImageView blurImgView;
    @FXML
    public AnchorPane loadPane;
    @FXML
    public ProgressIndicator bmProgress;
    private PixivClient client;
    private PixivArtwork artwork;
    private boolean bookmarked = false;

    private static FadeTransition getFadeTransition(Node node) {
        FadeTransition f = new FadeTransition();
        f.setNode(node);
        f.setDuration(Duration.millis(300));
        f.setOnFinished((u) -> node.setVisible(false));
        f.setFromValue(1.0);
        f.setToValue(0.0);
        return f;
    }

    public void nextPageBtnOnAction(ActionEvent actionEvent) {
    }

    public void prevPageBtnOnAction(ActionEvent actionEvent) {
    }

    public void followBtnOnAction(ActionEvent actionEvent) {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        root.widthProperty().addListener((a, b, c) -> {
            imgView.setFitWidth(c.doubleValue() - 200);
            blurImgView.setFitWidth(c.doubleValue() - 200);
        });
        root.heightProperty().addListener((a, b, c) -> {
            imgView.setFitHeight(c.doubleValue() - 200);
            blurImgView.setFitHeight(c.doubleValue() - 200);
        });
    }

    public void load(PixivClient client, PixivArtwork artwork) {
        if (client == null && artwork == null) return;

        LOG.info("Loading artwork {}", artwork.getId());
        this.client = client;
        this.artwork = artwork;
        this.bookmarked = artwork.isBookmarked();
        reloadBookmarkStatus();

        titleLbl.setText(artwork.getTitle());

        if (artwork.getXRestrict() == 1) {
            xResLbl.setVisible(true);
        } else {
            titleBox.getChildren().remove(0);
        }

        if (artwork.getDescription().trim().isEmpty()) {
            ((VBox) descView.getParent()).getChildren().remove(descView);
        } else {
            descView.setContextMenuEnabled(false);
            descView.getEngine().setUserStyleSheetLocation("data:,body{font: 12px Arial;}");
            descView.getEngine().loadContent(artwork.getDescription());
        }

        authorNameLbl.setText(artwork.getAuthor().getName());
        pubDateLbl.setText(artwork.getCreateDate());

        likeLbl.setText(String.valueOf(artwork.getLikeCount()));
        bookmarkLbl.setText(String.valueOf(artwork.getBookmarkCount()));

//        img.setFitWidth(artwork.getWidth());
//        img.setFitHeight(artwork.getHeight());

        loadProgress.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        Main.getTpe().submit(getAuthorImgRunnable(artwork));
        Main.getTpe().submit(getImageLoadRunnable(client, artwork));
        Main.getTpe().submit(getPreviewLoadRunnable(client, artwork));
    }

    private Runnable getPreviewLoadRunnable(PixivClient client, PixivArtwork artwork) {
        return () -> {
            try {
                Platform.runLater(() -> loadPane.setVisible(true));
                client.getFullPages(artwork);

                if (!artwork.isErrorOccurred()) {
                    final Image image;
                    URL url = new URL(artwork.getUrls().getString("small"));
                    URLConnection c;

                    if (client.getProxy() != null)
                        c = url.openConnection(client.getProxy());
                    else c = url.openConnection();

                    c.setRequestProperty("Referer", "https://www.pixiv.net");

                    Path tempFile = Files.createTempFile("zpixiv-", ".tmp");
                    tempFile.toFile().deleteOnExit();

                    var o = Files.newOutputStream(tempFile);
                    var b = new BufferedInputStream(c.getInputStream());
                    byte[] buffer = new byte[10240];
                    int size;
                    while ((size = b.read(buffer)) != -1) {
                        o.write(buffer, 0, size);
                    }
                    o.close();
                    b.close();

                    image = new Image(tempFile.toFile().toURI().toURL().toString(), true);
                    Platform.runLater(() -> {
                        if (imgView.getImage() != null) return;
                        imgView.setImage(image);
                        blurImgView.setImage(image);
                    });
                    LOG.info("Preview image loaded");
                }
            } catch (IOException e) {
                LOG.error("Exception in loading preview image", e);
                Main.showAlert("错误", "加载预览图像时出现错误");
                if (imgView.getImage() == null) return;
                getFadeTransition(loadPane).play();
            }
        };
    }

    private Runnable getImageLoadRunnable(PixivClient client, PixivArtwork artwork) {
        return () -> {
            try {
                client.getFullPages(artwork);

                if (!artwork.isErrorOccurred()) {
                    final Image image;
                    URL url = new URL(artwork.getImageUrls().get(0));
                    URLConnection c;

                    if (client.getProxy() != null)
                        c = url.openConnection(client.getProxy());
                    else c = url.openConnection();

                    c.setRequestProperty("Referer", "https://www.pixiv.net");

                    Path tempFile = Files.createTempFile("zpixiv-", ".tmp");
                    tempFile.toFile().deleteOnExit();

                    var o = Files.newOutputStream(tempFile);
                    var b = new BufferedInputStream(c.getInputStream());
                    int contentLength = c.getContentLength();
                    int curLength = 0;
                    byte[] buffer = new byte[10240];
                    int size;
                    final SimpleDoubleProperty s = new SimpleDoubleProperty(0);
                    Platform.runLater(() -> loadProgress.progressProperty().bind(s));
                    while ((size = b.read(buffer)) != -1) {
                        curLength += size;
                        s.set(Math.min((double) curLength / contentLength, 1.0));
                        o.write(buffer, 0, size);
                    }
                    o.close();
                    b.close();

                    image = new Image(tempFile.toFile().toURI().toURL().toString(), true);

                    FadeTransition f = getFadeTransition(loadPane);
                    Platform.runLater(() -> {
                        imgView.setImage(image);
                        f.play();
                    });
                    LOG.info("Image loaded");
                }
            } catch (IOException e) {
                Main.showAlert("错误", "加载作品时出现错误");
                LOG.error("Exception in loading image", e);
            }
        };
    }

    private Runnable getAuthorImgRunnable(PixivArtwork artwork) {
        final String profileImg = artwork.getAuthor().getProfileImg();
        return () -> {
            try {
                final Image image;
                InputStream is;
                URL url = new URL(profileImg);
                URLConnection c;

                if (client.getProxy() != null)
                    c = url.openConnection(client.getProxy());
                else c = url.openConnection();

                c.setRequestProperty("Referer", "https://www.pixiv.net");

                is = c.getInputStream();

                image = new Image(is);
                Platform.runLater(() -> authorImg.setImage(image));
                LOG.info("Author image loaded");
            } catch (IOException e) {
                LOG.error("Exception in author image", e);
            }
        };
    }

    @Override
    public void refresh() {
        LOG.info("Reloading artwork {}", artwork.getId());
        Main.getTpe().submit(getAuthorImgRunnable(artwork));
        Main.getTpe().submit(getImageLoadRunnable(client, artwork));
        Main.getTpe().submit(getPreviewLoadRunnable(client, artwork));
    }

    public void likeBtnOnAction() {
        Main.showAlert("赞~", "awa");
    }

    public void bookmarkBtnOnAction() {
        if (!bookmarked) {
            bmProgress.setVisible(true);
            Main.getTpe().submit(() -> {
                try {
                    bookmarked = client.addBookmark(artwork);
                } catch (Exception e) {
                    Main.showAlert("错误", "收藏失败");
                    LOG.error("Error bookmarking artwork", e);
                }

                reloadBookmarkStatus();
                Platform.runLater(() -> bmProgress.setVisible(false));
            });
        }
    }

    private void reloadBookmarkStatus() {
        bookmarkBtn.setStyle("-fx-shape: \"" + (bookmarked ? LikeSvg.FULL : LikeSvg.EMPTY) + "\";");
    }

    static class LikeSvg {
        public static final String EMPTY = "M667.786667 117.333333 C832.864 117.333333 938.666667 249.706667 938.666667 427.861333 c0 138.250667-125.098667 290.506667-371.573334 461.589334 a96.768 96.768 0 0 1-110.186666 0 C210.432 718.368 85.333333 566.112 85.333333 427.861333 85.333333 249.706667 191.136 117.333333 356.213333 117.333333 c59.616 0 100.053333 20.832 155.786667 68.096 C567.744 138.176 608.170667 117.333333 667.786667 117.333333 z m0 63.146667 c-41.44 0-70.261333 15.189333-116.96 55.04-2.165333 1.845333-14.4 12.373333-17.941334 15.381333 a32.32 32.32 0 0 1-41.770666 0 c-3.541333-3.018667-15.776-13.536-17.941334-15.381333-46.698667-39.850667-75.52-55.04-116.96-55.04 C230.186667 180.48 149.333333 281.258667 149.333333 426.698667 149.333333 537.6 262.858667 675.242667 493.632 834.826667 a32.352 32.352 0 0 0 36.736 0 C761.141333 675.253333 874.666667 537.6 874.666667 426.698667 c0-145.44-80.853333-246.218667-206.88-246.218667 z";
        public static final String FULL = "M667.786667 117.333333 C832.864 117.333333 938.666667 249.706667 938.666667 427.861333 c0 138.250667-125.098667 290.506667-371.573334 461.589334 a96.768 96.768 0 0 1-110.186666 0 C210.432 718.368 85.333333 566.112 85.333333 427.861333 85.333333 249.706667 191.136 117.333333 356.213333 117.333333 c59.616 0 100.053333 20.832 155.786667 68.096 C567.744 138.176 608.170667 117.333333 667.786667 117.333333 z";
    }
}