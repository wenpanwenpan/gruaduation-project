package com.wp.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Created by Administrator on 2019/5/9.
 * 关于地图导航的相关控制
 */
@Controller
public class MapController {

    @GetMapping(value = "/showmap/mymap")
    public String showMap(){

        System.out.println("收到地图请求");
        return "map";
    }

}
