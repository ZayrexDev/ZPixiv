package xyz.zcraft.zpixiv.util;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationHelper {
    public static FadeTransition getFadeOutTransition(Node node) {
        FadeTransition f = new FadeTransition();
        f.setNode(node);
        f.setDuration(Duration.millis(300));
        f.setOnFinished((u) -> node.setVisible(false));
        f.setFromValue(1.0);
        f.setToValue(0.0);
        return f;
    }

    public static FadeTransition getFadeInTransition(Node node) {
        FadeTransition f = new FadeTransition();
        f.setNode(node);
        f.setDuration(Duration.millis(300));
        f.setFromValue(0.0);
        f.setToValue(1.0);
        return f;
    }
}
