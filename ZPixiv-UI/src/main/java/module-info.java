module ZPixiv.UI {
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j.core;
    requires ZPixiv.API;
    requires javafx.web;
    requires ZPixiv;
    requires com.alibaba.fastjson2;

    exports xyz.zcraft.zpixiv.ui;
    exports xyz.zcraft.zpixiv.ui.controller;
    exports xyz.zcraft.zpixiv.ui.skin;
}