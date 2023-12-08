package xyz.zcraft.zpixiv.ui.controller;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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

    public static MsgController newInstance(String title, String msg) {
        try {
            FXMLLoader loader = new FXMLLoader(ResourceLoader.load("fxml/Alert.fxml"));
            loader.load();
            final MsgController controller = loader.getController();
            controller.setTexts(title, msg);
            controller.initTimeLine();
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

    public void initTimeLine() {
        tl.getKeyFrames().addAll(
                // Pane
                new KeyFrame(Duration.millis(0), new KeyValue(root.translateXProperty(), 135, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(100), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(100), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(root.translateXProperty(), 10, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(3000), new KeyValue(root.translateXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(3200), new KeyValue(root.translateXProperty(), 135, Interpolator.EASE_BOTH)),
                // Bar
                new KeyFrame(Duration.millis(100), new KeyValue(bar.endXProperty(), 0, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(3100), new KeyValue(bar.endXProperty(), 135, Interpolator.EASE_BOTH))
        );
        tl.setOnFinished(e -> ((VBox) root.getParent()).getChildren().remove(root));
        tl.setAutoReverse(false);
        tl.setCycleCount(1);
    }

    public void closeBtnOnAction() {
        tl.stop();
        tl.playFrom(Duration.millis(3000));
    }
}
