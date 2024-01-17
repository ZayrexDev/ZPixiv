package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.Identifier;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

public class ArtworkController implements Initializable, Refreshable, Closeable {
    private static final Logger LOG = LogManager.getLogger(ArtworkController.class);
    private final LinkedBlockingQueue<LoadTask> tasks = new LinkedBlockingQueue<>();
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
    public Region followBtn;
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
    public ScrollPane root;
    @FXML
    public AnchorPane imgAnchor;
    @FXML
    public ProgressBar loadProgressBar;
    @FXML
    public Label processLabel;
    @FXML
    public VBox loadPane;
    @FXML
    public Label likeTextLbl;
    @FXML
    public Label pageLbl;
    public AnchorPane pageWrapperPane;
    public FlowPane tagsPane;
    public Button downloadBtn;
    private PixivClient client;
    private volatile PixivArtwork artwork;
    private volatile LoadTask currentTask = null;
    private final TimerTask refreshTask = new TimerTask() {
        @Override
        public void run() {
            if (currentTask != null || !tasks.isEmpty()) refreshTasks();
        }
    };
    private CachedImage[] images;
    private int currentIndex = 1;
    private FadeTransition pageWrapperPaneFadeOutTransition;
    private FadeTransition pageWrapperPaneFadeInTransition;
    private Image previewImg;

    public void nextPageBtnOnAction() {
        if (currentIndex + 1 <= artwork.getOrigData().getPageCount()) {
            currentIndex++;
            updateImageIndex();
        }
    }

    public void prevPageBtnOnAction() {
        if (currentIndex >= 2) {
            currentIndex--;
            updateImageIndex();
        }
    }

    private void updateImageIndex() {
        Platform.runLater(() -> {
            pageLbl.setText(currentIndex + "/" + artwork.getOrigData().getPageCount());
            if (currentIndex == 1 && images[0] == null) {
                imgView.setImage(previewImg);
            } else {
                imgView.setImage(images[currentIndex - 1].getImage());
            }
            nextPageBtn.setDisable(currentIndex >= artwork.getOrigData().getPageCount());
            prevPageBtn.setDisable(currentIndex <= 1);
        });
    }

    public void followBtnOnAction() {

    }

    private synchronized void postLoadTask(LoadTask task) {
        tasks.add(task);
//        task.addListener(t -> refreshTasks());
        if (!loadPane.isVisible()) {
            final FadeTransition fadeInTransition = AnimationHelper.getFadeInTransition(loadPane);
            Platform.runLater(() -> {
                loadProgressBar.setProgress(task.getProgress());
                fadeInTransition.playFromStart();
                loadPane.setVisible(true);
            });
        }
        refreshTaskName();
    }

    private synchronized void refreshTasks() {
        tasks.removeIf(e -> e == null || e.isFailed() || e.isFinished());

        if (currentTask != null) {
            if (currentTask.isFinished() || currentTask.isFailed()) {
                currentTask = null;
                refreshTasks();
            }
        } else {
            if (tasks.isEmpty() || tasks.peek() == null) {
                Platform.runLater(() -> {
                    synchronized (this) {
                        AnimationHelper.getFadeOutTransition(loadPane).playFromStart();
                    }
                });
            } else {
                currentTask = tasks.poll();
                if (currentTask.isFinished() || currentTask.isFailed()) {
                    currentTask = null;
                    refreshTasks();
                } else {
                    Platform.runLater(() -> {
                        synchronized (this) {
                            if (currentTask == null) return;
                            processLabel.setText(currentTask.getName());
                            loadProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                            currentTask.addListener((v) -> Platform.runLater(() -> loadProgressBar.setProgress(v)));
                        }
                    });
                }
            }
            refreshTaskName();
        }
    }

    private void refreshTaskName() {
        String str = tasks.stream().map(LoadTask::getName)
                .reduce((s, s2) -> s.concat(" ").concat(s2))
                .orElse("");

        LOG.info("ORIG:{}", str);

        if (currentTask != null) {
            str = str.concat(" ").concat(currentTask.getName());
        }

        LOG.info("AFTER:{}", str);
        synchronized (this) {
            final String finalStr = str;
            Platform.runLater(() -> {
                synchronized (this) {
                    processLabel.setText(finalStr);
                }
            });
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        imgView.fitWidthProperty().bind(((HBox) imgView.getParent()).widthProperty());
        imgView.fitHeightProperty().bind(((HBox) imgView.getParent()).heightProperty());

        loadPane.setVisible(false);
        pageWrapperPane.setVisible(false);

        Rectangle r = new Rectangle(authorImg.getFitWidth(), authorImg.getFitHeight());
        r.setArcWidth(authorImg.getFitWidth());
        r.setArcHeight(authorImg.getFitHeight());
        authorImg.setClip(r);

        pageWrapperPaneFadeOutTransition = AnimationHelper.getFadeOutTransition(pageWrapperPane);
        pageWrapperPaneFadeOutTransition.setOnFinished(actionEvent -> pageWrapperPane.setVisible(false));
        pageWrapperPaneFadeInTransition = AnimationHelper.getFadeInTransition(pageWrapperPane);

        Main.getTimer().schedule(refreshTask, 0, 250);
    }

    public void load(PixivClient client, PixivArtwork art) {
        if (client == null && art == null) return;
        this.client = client;
        this.artwork = art;

        LOG.info("Loading artwork {}", artwork.getOrigData().getId());
        reloadArtworkStatus();

        titleLbl.setText(artwork.getOrigData().getTitle());

        if (artwork.getOrigData().getXRestrict() == 1) {
            xResLbl.setVisible(true);
        } else {
            titleBox.getChildren().remove(0);
        }

        if (artwork.getOrigData().getDescription().trim().isEmpty()) {
            ((VBox) descView.getParent()).getChildren().remove(descView);
        } else {
            Platform.runLater(() -> {
                descView.setContextMenuEnabled(false);
                descView.getEngine().setUserStyleSheetLocation("data:,body{font: 12px Arial;}");
                descView.getEngine().loadContent(this.artwork.getOrigData().getDescription());
            });
        }

        if (artwork.getAuthor() != null) authorNameLbl.setText(artwork.getAuthor().getName());
        pubDateLbl.setText(artwork.getOrigData().getCreateDate());

        likeLbl.setText(String.valueOf(artwork.getOrigData().getLikeCount()));
        bookmarkLbl.setText(String.valueOf(artwork.getOrigData().getBookmarkCount()));
        viewLbl.setText(String.valueOf(artwork.getOrigData().getViewCount()));

        images = new CachedImage[artwork.getOrigData().getPageCount()];

        if (artwork.getOrigData().getPageCount() <= 1) {
            pageWrapperPane.setVisible(false);
            pageLbl.setVisible(false);
        } else {
            prevPageBtn.setDisable(true);
            nextPageBtn.setDisable(artwork.getOrigData().getPageCount() == 1);
            pageLbl.setText("1/" + artwork.getOrigData().getPageCount());
        }

        loadProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);

        loadTags();

        Main.getTpe().submit(() -> {
            if (this.artwork.getAuthor() == null) {
                try {
                    final LoadTask t = new LoadTask("获取完整信息");
                    postLoadTask(t);
                    this.artwork = client.getFullData(art);
                    t.setFinished(true);
                    Platform.runLater(() -> {
                        likeLbl.setText(String.valueOf(artwork.getOrigData().getLikeCount()));
                        bookmarkLbl.setText(String.valueOf(artwork.getOrigData().getBookmarkCount()));
                        viewLbl.setText(String.valueOf(artwork.getOrigData().getViewCount()));
                    });
                } catch (IOException e) {
                    LOG.error("Failed to load artwork.", e);
                    Main.showAlert("错误", "加载失败");
                    return;
                }
            }
            Main.getTpe().submit(getAuthorImgRunnable());

            if (artwork.isGif()) {
                Main.getTpe().submit(getGifLoadRunnable());
            } else {
                Main.getTpe().submit(getImageLoadRunnable());
            }
        });

        LOG.info("Loading artwork {} complete", artwork.getOrigData().getId());
    }

    private Runnable getGifLoadRunnable() {
        return () -> {
            LOG.info("Loading gif artwork {}", artwork.getOrigData().getId());
            final LoadTask t = new LoadTask("加载动图");
            final LoadTask t1 = new LoadTask("解析动图");
            postLoadTask(t);
            postLoadTask(t1);

            Identifier identifier = Identifier.of(artwork.getOrigData().getId(), Identifier.Type.Gif, 0, Quality.Original);
            final CachedImage cachedImage;
            try {
                cachedImage = LoadHelper.loadImage(artwork.getGifData().getSrc(), identifier, 3, t);
                images[0] = cachedImage;
                Platform.runLater(() -> imgView.setImage(cachedImage.getImage()));
                t1.setFinished(true);
            } catch (Exception e) {
                LOG.error("Failed to parse gif.", e);
                t.setFailed(true);
                t1.setFailed(true);
                Main.showAlert("错误", "加载动图时出现错误");
            }
        };
    }

    private void loadTags() {
        artwork.getTags().forEach(tag -> {
            Hyperlink orig = new Hyperlink("#" + tag.getOrig());

            Label trans = new Label(tag.getTrans());
            trans.setTextFill(Color.GRAY);

            HBox tagBox = new HBox();
            tagBox.setPadding(new Insets(0, 0, 0, 2));
            tagBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
            tagBox.setPrefHeight(Region.USE_COMPUTED_SIZE);
            tagBox.setAlignment(Pos.CENTER);

            tagBox.getChildren().addAll(orig, trans);

            tagsPane.getChildren().add(tagBox);
        });
    }

    private Runnable getPreviewLoadRunnable() {
        return () -> {
            final LoadTask t = new LoadTask("加载预览");
            postLoadTask(t);
            final Identifier identifier = Identifier.of(artwork.getOrigData().getId(), Identifier.Type.Artwork, 0, Quality.ThumbMini);
            try {
                final CachedImage cachedImage = LoadHelper.loadImage(
                        Objects.requireNonNullElse(artwork.getOrigData().getUrls().getString(Quality.Small.getStr()),
                                artwork.getOrigData().getUrls().getString("1200x1200"))
                        , identifier, 1, t);
                previewImg = cachedImage.getImage();
                t.setFinished(true);
                updateImageIndex();
                LOG.info("Preview image loaded");
            } catch (Exception e) {
                LOG.error("Failed to load preview image", e);
            }
        };
    }

    private Runnable getImageLoadRunnable() {
        return () -> {
            final LoadTask t = new LoadTask("获取作品页面");
            postLoadTask(t);
            int tries = 3;
            while (tries >= 0) {
                try {
                    client.getFullPages(artwork);
                    LOG.info("Got {} pages for artwork {}", artwork.getOrigData().getPageCount(), artwork.getOrigData().getId());
                    Main.getTpe().submit(getPreviewLoadRunnable());
                    for (int i = 1; i <= artwork.getOrigData().getPageCount(); i++) {
                        Main.getTpe().submit(getImagePageLoadRunnable(i));
                    }
                    t.setFinished(true);

                    return;
                } catch (IOException e) {
                    tries--;
                    LOG.error("Exception in loading image pages", e);
                }
            }

            t.setFailed(true);

            Main.showAlert("错误", "加载作品页面时出现错误");
        };
    }

    private Runnable getImagePageLoadRunnable(int index) {
        return () -> {
            LOG.info("Loading page {} for artwork {}", index, artwork.getOrigData().getId());
            final LoadTask t = new LoadTask("获取作品(" + index + ")");
            postLoadTask(t);
            final Identifier identifier = Identifier.of(artwork.getOrigData().getId(), Identifier.Type.Artwork, index, Main.getConfig().getImageQuality());
            try {
                final CachedImage cachedImage = LoadHelper.loadImage(artwork.getImageUrls().get(index - 1).getString(Main.getConfig().getImageQuality().getStr()), identifier, 3, t);
                images[index - 1] = cachedImage;
                updateImageIndex();
                t.setFinished(true);
                LOG.info("Page {} for artwork {} loaded", index, artwork.getOrigData().getId());
                return;
            } catch (Exception e) {
                LOG.error("Failed to get page " + index, e);
            }

            t.setFailed(true);

            Main.showAlert("错误", "加载作品第" + index + "页时出现错误");
        };
    }

    private Runnable getAuthorImgRunnable() {
        return () -> {
            Platform.runLater(() -> authorNameLbl.setText(artwork.getAuthor().getName()));
            final Identifier identifier = Identifier.of(artwork.getAuthor().getId(), Identifier.Type.Profile, 0, Quality.Original);
            final CachedImage cachedImage = LoadHelper.loadImage(artwork.getAuthor().getProfileImg(), identifier, 1, null);
            Platform.runLater(() -> authorImg.setImage(cachedImage.getImage()));
            LOG.info("Author info loaded");
        };
    }

    @Override
    public void refresh() {
        LOG.info("Reloading artwork {}", artwork.getOrigData().getId());
        Main.getTpe().submit(getAuthorImgRunnable());
        Main.getTpe().submit(getImageLoadRunnable());
    }

    public void likeBtnOnAction() {
        if (!artwork.getOrigData().isLiked()) {
            final LoadTask likeTask = new LoadTask("赞!");
            postLoadTask(likeTask);
            likeBtn.setDisable(true);
            Main.getTpe().submit(() -> {
                try {
                    if (!client.likeArtwork(artwork)) throw new RuntimeException();
                    Main.showAlert("提示", "赞! 成功~");
                    likeTask.setFinished(true);
                } catch (Exception e) {
                    Main.showAlert("错误", "赞! 失败");
                    LOG.error("Error liking artwork", e);
                    likeTask.setFailed(true);
                }

                likeBtn.setDisable(false);
                reloadArtworkStatus();
            });
        }
    }

    public void bookmarkBtnOnAction() {
        if (!artwork.isBookmarked()) {
            final LoadTask bmTask = new LoadTask("收藏");
            postLoadTask(bmTask);
            bookmarkBtn.setDisable(true);
            Main.getTpe().submit(() -> {
                try {
                    if (!client.addBookmark(artwork)) throw new RuntimeException();
                    Main.showAlert("提示", "收藏成功~");
                    bmTask.setFinished(true);
                } catch (Exception e) {
                    Main.showAlert("错误", "收藏失败");
                    LOG.error("Error bookmarking artwork", e);
                    bmTask.setFailed(true);
                }

                bookmarkBtn.setDisable(false);
                reloadArtworkStatus();
            });
        }
    }

    private void reloadArtworkStatus() {
        if (artwork.isBookmarked()) {
            bookmarkBtn.setStyle("-fx-shape: \"" + SvgIcon.FULL + "\"; -fx-background-color: #ff4563;");
        } else bookmarkBtn.setStyle("");

        if (artwork.getOrigData().isLiked()) {
            likeBtn.setStyle("-fx-shape: \"" + SvgIcon.TICK + "\"; -fx-background-color: #48b3f7;");
            likeTextLbl.setStyle("-fx-text-fill: #48b3f7;");
        } else {
            likeBtn.setStyle("");
            likeTextLbl.setStyle("");
        }
    }

    public void hidePageWrapper() {
        if (artwork.getOrigData().getPageCount() <= 1) return;

        pageWrapperPaneFadeInTransition.stop();
        pageWrapperPaneFadeOutTransition.playFromStart();
    }

    public void showPageWrapper() {
        if (artwork.getOrigData().getPageCount() <= 1) return;

        pageWrapperPaneFadeOutTransition.stop();
        pageWrapperPaneFadeInTransition.playFromStart();
        pageWrapperPane.setVisible(true);
    }

    public void downloadBtnOnAction() {
        if (Arrays.stream(images).allMatch(Objects::nonNull)) {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("下载到文件夹...");
            final File file = chooser.showDialog(Main.getStage());
            if (file == null) return;
            try {
                saveImages(file.toPath());
                Main.showAlert("提示", "保存成功~");
            } catch (IOException e) {
                Main.showAlert("错误", "保存失败");
                LOG.error("Failed to save", e);
            }
        } else {
            Main.showAlert("提示", "请等待加载完成~");
        }
    }

    private void saveImages(Path file) throws IOException {
        Files.createDirectories(file);

        String ext;

        if (artwork.getOrigData().getIllustType() != 2) {
            ext = artwork.getImageUrls().get(0).getString(Main.getConfig().getImageQuality().getStr()).substring(artwork.getImageUrls().get(0).getString(Main.getConfig().getImageQuality().getStr()).lastIndexOf('.'));
        } else {
            ext = ".gif";
        }
        for (int i = 0, cachePathLength = images.length; i < cachePathLength; i++) {
            Path path = images[i].getPath();
            Files.copy(path, file.resolve(artwork.getOrigData().getId() + "_p" + i + ext));
        }
    }

    @Override
    public void close() {
        for (CachedImage image : images) {
            if (image != null) image.markUnused();
        }
        refreshTask.cancel();
    }

    public void openInspect() {
        if (Arrays.stream(images).allMatch(Objects::nonNull)) Main.openInspectStage(images);
    }
}

