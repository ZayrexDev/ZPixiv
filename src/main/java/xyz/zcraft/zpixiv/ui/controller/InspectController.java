package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import xyz.zcraft.zpixiv.util.CachedImage;

import java.net.URL;
import java.util.ResourceBundle;

import static xyz.zcraft.zpixiv.util.AnimationHelper.getFadeInTransition;
import static xyz.zcraft.zpixiv.util.AnimationHelper.getFadeOutTransition;

public class InspectController implements Initializable {
    public Button prevPageBtn;
    public Button nextPageBtn;
    public AnchorPane pageWrapperPane;
    public ImageView imgView;
    public Label pageLbl;
    public AnchorPane root;
    private Stage stage;
    private CachedImage[] images;
    private int currentIndex = 1;
    private FadeTransition pageWrapperPaneFadeOutTransition;
    private FadeTransition pWPTimeoutTrans;
    private FadeTransition pageWrapperPaneFadeInTransition;

    public void nextPageBtnOnAction() {
        if (currentIndex + 1 <= images.length) {
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
            pageLbl.setText(currentIndex + "/" + images.length);
            imgView.setImage(images[currentIndex - 1].getImage());
            nextPageBtn.setDisable(currentIndex >= images.length);
            prevPageBtn.setDisable(currentIndex <= 1);
        });
    }

    public void hidePageWrapper() {
        if (images.length <= 1) return;

        pageWrapperPaneFadeInTransition.stop();
        pageWrapperPaneFadeOutTransition.playFromStart();
        pWPTimeoutTrans.stop();
    }

    public void resetTimer() {
        pWPTimeoutTrans.stop();
        pWPTimeoutTrans.playFromStart();

        if (!pageWrapperPane.isVisible()) {
            showPageWrapper();
        }
    }

    public void showPageWrapper() {
        pWPTimeoutTrans.stop();
        pWPTimeoutTrans.playFromStart();

        if (images.length <= 1) return;

        pageWrapperPaneFadeOutTransition.stop();
        pageWrapperPaneFadeInTransition.playFromStart();
        pageWrapperPane.setVisible(true);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pageWrapperPaneFadeOutTransition = getFadeOutTransition(pageWrapperPane);
        pageWrapperPaneFadeOutTransition.setOnFinished(actionEvent -> pageWrapperPane.setVisible(false));
        pageWrapperPaneFadeInTransition = getFadeInTransition(pageWrapperPane);
        pWPTimeoutTrans = getFadeOutTransition(pageWrapperPane);
        pWPTimeoutTrans.setDelay(Duration.seconds(3));

        imgView.fitWidthProperty().bind(root.widthProperty());
        imgView.fitHeightProperty().bind(root.heightProperty());
    }

    public void init(Stage stage, CachedImage[] images) {
        this.images = images;
        currentIndex = 1;
        updateImageIndex();

        if (images.length <= 1) {
            pageWrapperPane.setVisible(false);
            pageLbl.setVisible(false);
        }

        this.stage = stage;
    }

    public void onKeyTyped(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case KP_DOWN -> nextPageBtnOnAction();
            case KP_UP -> prevPageBtnOnAction();
            case ESCAPE -> stage.close();
        }
    }

    public void onScrolled(ScrollEvent scrollEvent) {
        if (scrollEvent.getDeltaY() < 0) {
            nextPageBtnOnAction();
        } else if (scrollEvent.getDeltaY() > 0) {
            prevPageBtnOnAction();
        }
    }
}
