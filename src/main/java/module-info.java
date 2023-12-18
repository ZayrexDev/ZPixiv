module ZPixiv {
    requires lombok;
    requires com.alibaba.fastjson2;
    requires org.apache.logging.log4j;
    requires org.jsoup;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j.core;
    requires javafx.web;
    requires animated.gif.lib;
    requires java.desktop;
    requires eu.iamgio.animated;
    requires AnimateFX;

    exports xyz.zcraft.zpixiv.api.artwork;
    exports xyz.zcraft.zpixiv.api.user;
    exports xyz.zcraft.zpixiv.api;
    exports xyz.zcraft.zpixiv.ui;
    exports xyz.zcraft.zpixiv.ui.controller;
}