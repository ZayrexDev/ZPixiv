package xyz.zcraft.zpixiv.util;

import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;

public class LayoutUtil {
    public static Node fillAnchor(Node node) {
        AnchorPane.setTopAnchor(node, 0d);
        AnchorPane.setBottomAnchor(node, 0d);
        AnchorPane.setLeftAnchor(node, 0d);
        AnchorPane.setRightAnchor(node, 0d);
        return node;
    }
}
