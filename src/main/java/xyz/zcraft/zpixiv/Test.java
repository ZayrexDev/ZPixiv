package xyz.zcraft.zpixiv;

import xyz.zcraft.zpixiv.api.PixivClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class Test {
    public static void main(String[] args) throws IOException {
        PixivClient client = new PixivClient("first_visit_datetime_pc=2024-01-28%2009%3A10%3A20; cc1=2024-01-28%2009%3A10%3A20; p_ab_id=8; p_ab_id_2=8; p_ab_d_id=455650151; yuid_b=ODdVNjc; __cf_bm=DXBk79ssXIGCX6jCBruxyE9bv3d6_bhdp.2_5azADPM-1706400620-1-AXgWr7L68xWKPhYP8ZNlrKeQMZhA6jfPgKl0vuJBBFMlzbkpy5c9KU8KXFTgR3uLaYIXk7ow+6qWj7eDgLDpNgsZfMB80zYHYSHz0OwGhI3u; cf_clearance=Bt.jJzLCQVRmqI9MqGx5kEsiyGuvhL0vPtrtoVkgDMY-1706400623-1-AdtwbyEDNry9m99717KuuOe0ApCBo5PjzwmvEyF8CBeVFUONEgk0TtLEP/b5UYZTkjrnFvAeiV2oZv25yfseUIo=; PHPSESSID=88458885_pBTMKxbB9oiBVinGTuoNuofbvX4Mxyje; device_token=c8e9f1dc37903927707516fa09053232; privacy_policy_agreement=6; c_type=24; privacy_policy_notification=0; a_type=0; b_type=1; QSI_S_ZN_5hF4My7Ad6VNNAi=v:0:0",
                new Proxy(Proxy.Type.HTTP, new InetSocketAddress(7890)), true);
        client.getUserTopArtworks("10548814").forEach(System.out::println);
    }
}
