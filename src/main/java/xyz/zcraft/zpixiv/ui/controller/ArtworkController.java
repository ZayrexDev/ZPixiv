package xyz.zcraft.zpixiv.ui.controller;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.PixivClient;
import xyz.zcraft.zpixiv.api.artwork.PixivArtwork;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.ui.util.Refreshable;

import javax.imageio.ImageIO;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

public class ArtworkController implements Initializable, Refreshable {
    private static final Logger LOG = LogManager.getLogger(ArtworkController.class);
    private final LinkedList<LoadTask> tasks = new LinkedList<>();
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
    private PixivArtwork artwork;
    private volatile LoadTask currentTask = null;
    private Image[] images;
    private int currentIndex = 1;
    private FadeTransition pageWrapperPaneFadeOutTransition;
    private FadeTransition pageWrapperPaneFadeInTransition;
    private Image previewImg;
    private Path[] cachePath = null;

    private static FadeTransition getFadeOutTransition(Node node) {
        FadeTransition f = new FadeTransition();
        f.setNode(node);
        f.setDuration(Duration.millis(300));
        f.setOnFinished((u) -> node.setVisible(false));
        f.setFromValue(1.0);
        f.setToValue(0.0);
        return f;
    }

    private static FadeTransition getFadeInTransition(Node node) {
        FadeTransition f = new FadeTransition();
        f.setNode(node);
        f.setDuration(Duration.millis(300));
        f.setFromValue(0.0);
        f.setToValue(1.0);
        return f;
    }

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
                imgView.setImage(images[currentIndex - 1]);
            }
            nextPageBtn.setDisable(currentIndex >= artwork.getOrigData().getPageCount());
            prevPageBtn.setDisable(currentIndex <= 1);
        });
    }

    public void followBtnOnAction() {

    }

    private void postLoadTask(LoadTask task) {
        tasks.add(task);
        task.addListener(t -> refreshTasks());
        if (!loadPane.isVisible()) {
            final FadeTransition fadeInTransition = getFadeInTransition(loadPane);
            Platform.runLater(() -> {
                fadeInTransition.playFromStart();
                loadPane.setVisible(true);
            });
        }
        refreshTasks();
    }

    private synchronized void refreshTasks() {
        tasks.removeIf(LoadTask::isFinished);

        if (currentTask != null) {
            if (currentTask.isFinished() || currentTask.isFailed()) {
                currentTask = null;
                refreshTasks();
            }
        } else {
            if (tasks.isEmpty() || tasks.getFirst() == null) {
                Platform.runLater(() -> getFadeOutTransition(loadPane).playFromStart());
                return;
            } else {
                currentTask = tasks.removeFirst();
                if (currentTask.isFinished() || currentTask.isFailed()) {
                    currentTask = null;
                    refreshTasks();
                } else {
                    Platform.runLater(() -> {
                        processLabel.setText(currentTask.getName());
                        loadProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        currentTask.addListener((v) -> Platform.runLater(() -> loadProgressBar.setProgress(v)));
                    });
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            sb.append(tasks.get(i).getName()).append(" ");
        }
        if (currentTask != null) sb.append(currentTask.getName());
        Platform.runLater(() -> processLabel.setText(sb.toString()));
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

        pageWrapperPaneFadeOutTransition = getFadeOutTransition(pageWrapperPane);
        pageWrapperPaneFadeOutTransition.setOnFinished(actionEvent -> pageWrapperPane.setVisible(false));
        pageWrapperPaneFadeInTransition = getFadeInTransition(pageWrapperPane);
    }

    public void load(PixivClient client, PixivArtwork artwork) {
        if (client == null && artwork == null) return;

        LOG.info("Loading artwork {}", artwork.getOrigData().getId());
        this.client = client;
        this.artwork = artwork;
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
                descView.getEngine().loadContent(artwork.getOrigData().getDescription());
            });
        }

        authorNameLbl.setText(artwork.getAuthor().getName());
        pubDateLbl.setText(artwork.getOrigData().getCreateDate());

        likeLbl.setText(String.valueOf(artwork.getOrigData().getLikeCount()));
        bookmarkLbl.setText(String.valueOf(artwork.getOrigData().getBookmarkCount()));
        viewLbl.setText(String.valueOf(artwork.getOrigData().getViewCount()));

        images = new Image[artwork.getOrigData().getPageCount()];
        cachePath = new Path[artwork.getOrigData().getPageCount()];

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

        Main.getTpe().submit(getPreviewLoadRunnable());
        Main.getTpe().submit(getAuthorImgRunnable());

        if (artwork.isGif()) {
            Main.getTpe().submit(getGifLoadRunnable());
        } else {
            Main.getTpe().submit(getImageLoadRunnable());
        }

        LOG.info("Loading artwork {} complete", artwork.getOrigData().getId());
    }

    private Runnable getGifLoadRunnable() {
        return () -> {
            LOG.info("Loading gif artwork {}", artwork.getOrigData().getId());
            final LoadTask t = new LoadTask("加载动图");
            final LoadTask t1 = new LoadTask("解析动图");
            postLoadTask(t);
            postLoadTask(t1);
            int tries = 3;

            while (tries-- >= 1) {
                try {
                    client.getGifData(artwork);

                    URL url = new URL(artwork.getGifData().getSrc());

                    URLConnection c;
                    if (client.getProxy() != null)
                        c = url.openConnection(client.getProxy());
                    else c = url.openConnection();

                    c.setRequestProperty("Referer", PixivClient.Urls.REFERER);

                    final InputStream inputStream = c.getInputStream();
                    ZipInputStream zis = new ZipInputStream(inputStream);

                    AnimatedGifEncoder age = new AnimatedGifEncoder();
                    age.setRepeat(0);
                    age.setDelay(artwork.getGifData().getOrigFrame().getJSONObject(0).getInteger("delay"));

                    final Path tempFile = Files.createTempFile("zpixiv-", ".tmp");
                    tempFile.toFile().deleteOnExit();

                    age.start(Files.newOutputStream(tempFile));

                    final double total = artwork.getGifData().getOrigFrame().size();
                    double cur = 0;
                    while (zis.getNextEntry() != null) {
                        age.addFrame(ImageIO.read(zis));
                        t.setProgress(++cur / total);
                    }

                    age.finish();

                    t.setFinished(true);

                    final Image image = new Image(Files.newInputStream(tempFile));
                    images[0] = image;
                    cachePath[0] = tempFile;

                    Platform.runLater(() -> imgView.setImage(image));

                    t1.setFinished(true);
                } catch (IOException e) {
                    LOG.error("Can't load gif data", e);
                }
            }

            t.setFailed(true);
            t1.setFailed(true);
            Main.showAlert("错误", "加载动图时出现错误");
        };
    }

    private void loadTags() {
        artwork.getTags().forEach(tag -> {
            Hyperlink orig = new Hyperlink("#" + tag.orig());

            Label trans = new Label(tag.trans());
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
            try {
                client.getFullPages(artwork);

                URL url = new URL(artwork.getOrigData().getUrls().getString("small"));
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
                int contentLength = c.getContentLength();
                int curLength = 0;
                while ((size = b.read(buffer)) != -1) {
                    o.write(buffer, 0, size);
                    curLength += size;
                    t.setProgress(Math.min((double) curLength / contentLength, 1.0));
                }
                o.close();
                b.close();

                previewImg = new Image(tempFile.toFile().toURI().toURL().toString(), true);
                t.setFinished(true);
                updateImageIndex();
                LOG.info("Preview image loaded");
            } catch (IOException e) {
                LOG.error("Exception in loading preview image", e);
                Main.showAlert("错误", "加载预览图像时出现错误");
                t.setFailed(true);
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
            int tries = 3;
            while (tries >= 0) {
                try {
                    URL url = new URL(artwork.getImageUrls().get(index - 1));

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
                    while ((size = b.read(buffer)) != -1) {
                        curLength += size;
                        t.setProgress(Math.min((double) curLength / contentLength, 1.0));
                        o.write(buffer, 0, size);
                    }
                    o.close();
                    b.close();

                    final Image image = new Image(tempFile.toFile().toURI().toURL().toString());

                    images[index - 1] = image;
                    cachePath[index - 1] = tempFile;

                    updateImageIndex();

                    t.setFinished(true);
                    LOG.info("Page {} for artwork {} loaded", index, artwork.getOrigData().getId());
                    return;
                } catch (IOException e) {
                    LOG.error("Failed to get page " + index, e);
                    tries++;
                }
            }

            t.setFailed(true);

            Main.showAlert("错误", "加载作品第" + index + "页时出现错误");
        };
    }

    private Runnable getAuthorImgRunnable() {
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
        LOG.info("Reloading artwork {}", artwork.getOrigData().getId());
        Main.getTpe().submit(getPreviewLoadRunnable());
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

        if(artwork.getOrigData().getIllustType() != 2) {
            ext = artwork.getImageUrls().get(0).substring(artwork.getImageUrls().get(0).lastIndexOf('.'));
        } else {
            ext = ".gif";
        }
        for (int i = 0, cachePathLength = cachePath.length; i < cachePathLength; i++) {
            Path path = cachePath[i];
            Files.copy(path, file.resolve(artwork.getOrigData().getId() + "_p" + i + ext));
        }
    }

    @SuppressWarnings("unused")
    static class SvgIcon {
        public static final String EMPTY = "M667.786667 117.333333 C832.864 117.333333 938.666667 249.706667 938.666667 427.861333 c0 138.250667-125.098667 290.506667-371.573334 461.589334 a96.768 96.768 0 0 1-110.186666 0 C210.432 718.368 85.333333 566.112 85.333333 427.861333 85.333333 249.706667 191.136 117.333333 356.213333 117.333333 c59.616 0 100.053333 20.832 155.786667 68.096 C567.744 138.176 608.170667 117.333333 667.786667 117.333333 z m0 63.146667 c-41.44 0-70.261333 15.189333-116.96 55.04-2.165333 1.845333-14.4 12.373333-17.941334 15.381333 a32.32 32.32 0 0 1-41.770666 0 c-3.541333-3.018667-15.776-13.536-17.941334-15.381333-46.698667-39.850667-75.52-55.04-116.96-55.04 C230.186667 180.48 149.333333 281.258667 149.333333 426.698667 149.333333 537.6 262.858667 675.242667 493.632 834.826667 a32.352 32.352 0 0 0 36.736 0 C761.141333 675.253333 874.666667 537.6 874.666667 426.698667 c0-145.44-80.853333-246.218667-206.88-246.218667 z";
        public static final String FULL = "M667.786667 117.333333 C832.864 117.333333 938.666667 249.706667 938.666667 427.861333 c0 138.250667-125.098667 290.506667-371.573334 461.589334 a96.768 96.768 0 0 1-110.186666 0 C210.432 718.368 85.333333 566.112 85.333333 427.861333 85.333333 249.706667 191.136 117.333333 356.213333 117.333333 c59.616 0 100.053333 20.832 155.786667 68.096 C567.744 138.176 608.170667 117.333333 667.786667 117.333333 z";
        public static final String TICK = "M5,7.08578644 L9.29289322,2.79289322 C9.68341751,2.40236893 10.3165825,2.40236893 10.7071068,2.79289322 C11.0976311,3.18341751 11.0976311,3.81658249 10.7071068,4.20710678 L5,9.91421356 L2.29289322,7.20710678 C1.90236893,6.81658249 1.90236893,6.18341751 2.29289322,5.79289322 C2.68341751,5.40236893 3.31658249,5.40236893 3.70710678,5.79289322 L5,7.08578644 Z";
        public static final String SMILE = "M2,6 C0.8954305,6 0,5.1045695 0,4 C0,2.8954305 0.8954305,2 2,2 C3.1045695,2 4,2.8954305 4,4 C4,5.1045695 3.1045695,6 2,6 Z M10,6 C8.8954305,6 8,5.1045695 8,4 C8,2.8954305 8.8954305,2 10,2 C11.1045695,2 12,2.8954305 12,4 C12,5.1045695 11.1045695,6 10,6 Z M2.1109127,8.8890873 C1.72038841,8.498563 1.72038841,7.86539803 2.1109127,7.47487373 C2.501437,7.08434944 3.13460197,7.08434944 3.52512627,7.47487373 C4.89196129,8.84170876 7.10803871,8.84170876 8.47487373,7.47487373 C8.86539803,7.08434944 9.498563,7.08434944 9.8890873,7.47487373 C10.2796116,7.86539803 10.2796116,8.498563 9.8890873,8.8890873 C7.74120369,11.0369709 4.25879631,11.0369709 2.1109127,8.8890873 Z";
    }
}

@Getter
@RequiredArgsConstructor
final class LoadTask {
    private final String name;
    private final LinkedList<Consumer<Double>> changeListeners = new LinkedList<>();
    private double progress = -1;
    private boolean failed = false;
    private boolean finished = false;

    public void setProgress(double progress) {
        this.progress = progress;
        fireChanged();
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
        fireChanged();
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
        fireChanged();
    }

    public void addListener(Consumer<Double> lis) {
        changeListeners.add(lis);
    }

    private void fireChanged() {
        Main.getTpe().submit(() -> changeListeners.forEach(e -> e.accept(progress)));
    }

    @Override
    public String toString() {
        return "LoadTask{" +
                "name='" + name + '\'' +
                ", progress=" + progress +
                ", failed=" + failed +
                ", finished=" + finished +
                '}';
    }
}