package xyz.zcraft.zpixiv.ui.container;


import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;

public class AnimatedFlowPane extends FlowPane {
    public static final Duration DURATION = Duration.millis(150);

    public AnimatedFlowPane() {
        super();

        getChildren().addListener((ListChangeListener<Node>) c -> {
            c.next();
            for (int i = 0; i < c.getAddedSubList().size(); i++) {
                final Node node = c.getAddedSubList().get(i);
                node.layoutXProperty().addListener((observable, oldValue, newValue) -> {
                    var t = new TranslateTransition();
                    t.setNode(node);
                    t.setDuration(DURATION);
                    t.setInterpolator(Interpolator.EASE_BOTH);
                    t.setFromX(oldValue.doubleValue() - newValue.doubleValue());
                    t.setToX(0);
                    node.setTranslateX(oldValue.doubleValue() - newValue.doubleValue());
                    t.playFromStart();
                });
                node.layoutYProperty().addListener((observable, oldValue, newValue) -> {
                    var t = new TranslateTransition();
                    t.setNode(node);
                    t.setDuration(Duration.millis(100));
                    t.setInterpolator(Interpolator.EASE_BOTH);
                    t.setFromY(oldValue.doubleValue() - newValue.doubleValue());
                    t.setToY(0);
                    node.setTranslateY(oldValue.doubleValue() - newValue.doubleValue());
                    t.playFromStart();
                });
            }
        });
    }
}

