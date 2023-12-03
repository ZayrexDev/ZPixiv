module ZPixiv.API {
    requires lombok;
    requires com.alibaba.fastjson2;
    requires org.apache.logging.log4j;
    requires org.jsoup;

    exports xyz.zcraft.zpixiv.api.artwork;
    exports xyz.zcraft.zpixiv.api.user;
    exports xyz.zcraft.zpixiv.api;
}