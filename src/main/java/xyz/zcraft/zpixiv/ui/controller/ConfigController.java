package xyz.zcraft.zpixiv.ui.controller;

import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.zcraft.zpixiv.api.artwork.Quality;
import xyz.zcraft.zpixiv.ui.Main;
import xyz.zcraft.zpixiv.util.Config;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

public class ConfigController implements Initializable {
    private static final Logger LOG = LogManager.getLogger();
    public ComboBox<Quality> qualityCombo;
    public RadioButton noProxyCombo;
    public RadioButton proxyCombo;
    public HBox proxyPane;
    public TextField proxyHostField;
    public TextField proxyPortField;
    public TextField cacheSizeField;
    public ToggleGroup proxy;

    public void proxyComboChanged() {
        proxyPane.setDisable(noProxyCombo.isSelected());
    }

    public void saveConfig() {
        Config config = Main.getConfig();
        config.setMaxCacheSize(Integer.parseInt(cacheSizeField.getText()));
        config.setImageQuality(qualityCombo.getSelectionModel().getSelectedItem());
        if (noProxyCombo.isSelected()) {
            config.setProxyPort(null);
            config.setProxyHost(null);
        } else {
            config.setProxyPort(proxyPortField.getText());
            config.setProxyHost(proxyHostField.getText());
        }
        try {
            Main.saveConfig();
            Main.showAlert("提示", "保存成功");
        } catch (Exception e) {
            Main.showAlert("错误", "保存失败");
            LOG.error("Error in saving config.", e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        qualityCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Quality quality) {
                switch (quality) {
                    case ThumbMini -> {
                        return "极小";
                    }
                    case Small -> {
                        return "小";
                    }
                    case Regular -> {
                        return "中等";
                    }
                    case Original -> {
                        return "原图";
                    }
                    default -> throw new AssertionError();
                }
            }

            @Override
            public Quality fromString(String s) {
                switch (s) {
                    case "极小" -> {
                        return Quality.ThumbMini;
                    }
                    case "小" -> {
                        return Quality.Small;
                    }
                    case "中等" -> {
                        return Quality.Regular;
                    }
                    case "原图" -> {
                        return Quality.Original;
                    }
                    default -> throw new AssertionError();
                }
            }
        });
        cacheSizeField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getText();
            if (text.matches("[0-9]*")) {
                return change;
            }
            return null;
        }));
        qualityCombo.getItems().addAll(Quality.values());

        Config config = Main.getConfig();
        qualityCombo.getSelectionModel().select(config.getImageQuality());
        cacheSizeField.setText(String.valueOf(config.getMaxCacheSize()));

        boolean proxy = !(Objects.isNull(config.getProxyPort()) || config.getProxyPort().isBlank());
        noProxyCombo.setSelected(!proxy);
        proxyCombo.setSelected(proxy);
        proxyPane.setDisable(!proxy);
        proxyPortField.setText(config.getProxyPort());
        proxyHostField.setText(config.getProxyHost());
    }
}
