package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
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
    public ImageView imgView;
    public Button nextPageBtn;
    public Button prevPageBtn;
    public Label titleLbl;
    public ImageView authorImg;
    public Label authorNameLbl;
    public Label pubDateLbl;
    public Button followBtn;
    public Label likeLbl;
    public Label bookmarkLbl;
    public Label viewLbl;
    public Region likeBtn;
    public Region bookmarkBtn;
    public Region hidbkBtn;
    public Label xResLbl;
    public HBox titleBox;
    public WebView descView;
    public VBox root;
    public ProgressIndicator loadProgress;
    public AnchorPane imgAnchor;
    public ImageView blurImgView;
    public AnchorPane loadPane;
    public Region bmIcon;
    public Region viewIcon;
    public Region likeIcon;

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
        root.widthProperty().addListener((a, b, c) -> {
            imgView.setFitWidth(c.doubleValue() - 200);
            blurImgView.setFitWidth(c.doubleValue() - 200);
        });
        root.heightProperty().addListener((a, b, c) -> {
            imgView.setFitHeight(c.doubleValue() - 200);
            blurImgView.setFitHeight(c.doubleValue() - 200);
        });
    }


    private PixivClient client;
    private PixivArtwork artwork;

    public void load(PixivClient client, PixivArtwork artwork) {
        LOG.info("Loading artwork {}", artwork.getId());
        this.client = client;
        this.artwork = artwork;

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

        Thread authorImgThread = getAuthorImgThread(artwork);
        authorImgThread.start();

        Thread imageLoadThread = getImageLoadThread(client, artwork);
        imageLoadThread.start();

        Thread previewLoadThread = getPreviewLoadThread(client, artwork);
        previewLoadThread.start();
    }

    private Thread getPreviewLoadThread(PixivClient client, PixivArtwork artwork) {
        Thread previewLoadThread = new Thread(() -> {
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

                if (imgView.getImage() == null) return;
                getFadeTransition(loadPane).play();
            }
        });
        previewLoadThread.setDaemon(true);
        return previewLoadThread;
    }

    private static FadeTransition getFadeTransition(Node node) {
        FadeTransition f = new FadeTransition();
        f.setNode(node);
        f.setDuration(Duration.millis(300));
        f.setOnFinished((u) -> node.setVisible(false));
        f.setFromValue(1.0);
        f.setToValue(0.0);
        return f;
    }

    private Thread getImageLoadThread(PixivClient client, PixivArtwork artwork) {
        Thread imageLoadThread = new Thread(() -> {
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
                LOG.error("Exception in loading image", e);
            }
        });
        imageLoadThread.setDaemon(true);
        return imageLoadThread;
    }

    private Thread getAuthorImgThread(PixivArtwork artwork) {
        final String profileImg = artwork.getAuthor().getProfileImg();
        Thread authorImgThread = new Thread(() -> {
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
        });
        authorImgThread.setDaemon(true);
        return authorImgThread;
    }

    @Override
    public void refresh() {
        LOG.info("Reloading artwork {}", artwork.getId());
        Thread authorImgThread = getAuthorImgThread(artwork);
        authorImgThread.start();

        Thread imageLoadThread = getImageLoadThread(client, artwork);
        imageLoadThread.start();

        Thread previewLoadThread = getPreviewLoadThread(client, artwork);
        previewLoadThread.start();
    }
}