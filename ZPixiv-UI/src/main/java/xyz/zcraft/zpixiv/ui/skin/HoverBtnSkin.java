package xyz.zcraft.zpixiv.ui.skin;

import javafx.animation.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.skin.ButtonSkin;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class HoverBtnSkin extends ButtonSkin {
    private static final Color hovered = Color.rgb(255, 255, 255, 0.1);
    private static final Color not = Color.rgb(0, 0, 0, 0);
    public HoverBtnSkin(Button button) {
        super(button);

        SimpleObjectProperty<Color> color = new SimpleObjectProperty<>(null);
        color.addListener(o -> button.setBackground(new Background(new BackgroundFill(color.get(), new CornerRadii(0), new Insets(0)))));

        Timeline in = new Timeline(
                new KeyFrame(Duration.millis(0), "op", new KeyValue(color, not)),
                new KeyFrame(Duration.millis(100), "show", new KeyValue(color, hovered))
        );

        in.setAutoReverse(false);
        in.setCycleCount(0);

        Timeline out = new Timeline(
                new KeyFrame(Duration.millis(200), "op", new KeyValue(color, not))
        );

        out.setAutoReverse(false);
        out.setCycleCount(0);

        color.set(hovered);
        button.setOnMouseEntered(e -> {
            out.stop();
            in.playFromStart();
        });

        button.setOnMouseExited(e -> {
            in.stop();
            out.playFromStart();
        });
    }
}
