package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import lombok.Getter;
import xyz.zcraft.zpixiv.ui.util.ResourceLoader;

import java.io.IOException;

public class MsgController {
    @FXML
    public Label titleLbl;
    @FXML
    public Label msgLbl;
    @FXML
    @Getter
    public AnchorPane root;

    @FXML
    @Getter
    public Line bar;

    private volatile boolean closed = false;

    public static MsgController newInstance(String title, String msg) {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Alert.fxml"));
            loader.load();
            final MsgController controller = loader.getController();
            controller.setTexts(title, msg);
            controller.initAnimation();
            return controller;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setTexts(String title, String msg) {
        titleLbl.setText(title);
        msgLbl.setText(msg);
    }

    public void playTimeLine() {
        tl.playFromStart();
        root.setVisible(true);
    }

    private final Timeline tl = new Timeline();
    private final Timeline tReverse = new Timeline();
    private final TranslateTransition tt = new TranslateTransition();
    private final TranslateTransition ttr = new TranslateTransition();
    private final TranslateTransition hide = new TranslateTransition();

    public void initAnimation() {
        tl.getKeyFrames().addAll(
                // Pane
                new KeyFrame(Duration.millis(0), new KeyValue(root.translateXProperty(), 135, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(100), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(100), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(root.translateXProperty(), 10, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(3000), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                // Bar
                new KeyFrame(Duration.millis(200), new KeyValue(bar.opacityProperty(), 1)),
                new KeyFrame(Duration.millis(100), new KeyValue(bar.endXProperty(), 0)),
                new KeyFrame(Duration.millis(3100), new KeyValue(bar.endXProperty(), 135, Interpolator.EASE_BOTH))
        );
        tl.setOnFinished(e -> hide.playFromStart());
        tl.setAutoReverse(false);
        tl.setCycleCount(1);

        tReverse.getKeyFrames().addAll(
                new KeyFrame(Duration.millis(200), new KeyValue(bar.endXProperty(), 0, Interpolator.EASE_IN)),
                new KeyFrame(Duration.millis(200), new KeyValue(bar.opacityProperty(), 0))
        );

        tt.setNode(root);
        tt.setFromX(0);
        tt.setToX(-10);
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.setDuration(Duration.millis(100));

        ttr.setNode(root);
        ttr.setFromX(-10);
        ttr.setToX(0);
        ttr.setInterpolator(Interpolator.EASE_BOTH);
        ttr.setDuration(Duration.millis(100));

        hide.setNode(root);
        hide.setToX(135);
        hide.setInterpolator(Interpolator.EASE_BOTH);
        hide.setDuration(Duration.millis(150));
        hide.setOnFinished(e -> ((VBox) root.getParent()).getChildren().remove(root));
    }

    public void closeBtnOnAction() {
        closed = true;
        tl.stop();
//        tl.playFrom(Duration.millis(3000));
        hide.playFromStart();
    }

    public void mouseEntered() {
        if(closed || tl.getCurrentTime().greaterThan(Duration.millis(3000))) return;
        tl.pause();
        tReverse.playFromStart();
        tt.playFromStart();
    }

    public void mouseExited() {
        if(closed) return;
        tReverse.stop();
        tl.playFrom(Duration.millis(300));
        ttr.playFromStart();
    }
}
