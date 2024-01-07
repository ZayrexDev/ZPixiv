module ZPixiv {
    requires lombok;
    requires com.alibaba.fastjson2;
    requires org.apache.logging.log4j;
    requires org.jsoup;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.base;
    requires javafx.web;
    requires animated.gif.lib;
    requires java.desktop;
    requires org.jetbrains.annotations;

    exports xyz.zcraft.zpixiv.api.artwork;
    exports xyz.zcraft.zpixiv.api.user;
    exports xyz.zcraft.zpixiv.api;
    exports xyz.zcraft.zpixiv.ui;
    exports xyz.zcraft.zpixiv.ui.controller;
    exports xyz.zcraft.zpixiv.util;
}