package com.wp.demo.controller;

import com.github.pagehelper.Page;
import com.wp.demo.bean.Commodity;
import com.wp.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by Administrator on 2019/4/26.
 * 该类为微信小程序所使用
 */
@RestController
public class MiniController {

    @Autowired
    ProductService productService;

    /**
     * 取得所有的商品
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/getAllCommodity")
    public List<Commodity> getCommodity() throws Exception {
        Page<Commodity> all = productService.findAll();
        return all;
    }

    /**
     * 按key进行条件搜索模糊查询
     * @param key
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/searchByKey")
    public List<Commodity> getCommodityByKey(@RequestParam(value = "key") String key) throws Exception {

        List<Commodity> list = productService.seachCommodity(key);

        return list;
    }

}
