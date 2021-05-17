package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {
    @Autowired
    private IndexService indexService;

    @GetMapping({"index.html","/"})
    public String toIndex(Model model){
        List<CategoryEntity> categoryEntities = this.indexService.queryLv1CategoriesByPid();
        model.addAttribute("categories",categoryEntities);
        return "index";
    }
    @ResponseBody
    @GetMapping("index/cates/{pid}")
    public ResponseVo<List<CategoryEntity>>queryLv12CategoriesWithSubByPid(@PathVariable("pid") Long pid){
        List<CategoryEntity>listResponseVo =  this.indexService.queryLv12CategoriesWithSubByPid(pid);
        return ResponseVo.ok(listResponseVo);

    }
    @GetMapping("index/test/lock")
    @ResponseBody
    public ResponseVo testLock() {
        this.indexService.testLock();
        return ResponseVo.ok();


    }
    @GetMapping("index/test/read")
    @ResponseBody
    public ResponseVo testRead() {
        this.indexService.testRead();
        return ResponseVo.ok("读取成功");
    }
    @GetMapping("index/test/write")
    @ResponseBody
    public ResponseVo testWrite() {
        this.indexService.testWrite();
        return ResponseVo.ok("写入成功");
    }
    @GetMapping("index/test/countdown")
    @ResponseBody
    public ResponseVo testCountdown() {
        this.indexService.testCountdown();
        return ResponseVo.ok("出来了一位同学");
    }
    @GetMapping("index/test/latch")
    @ResponseBody
    public ResponseVo testLatch() {
        this.indexService.testLatch();
        return ResponseVo.ok("班长锁门了");
    }




}
