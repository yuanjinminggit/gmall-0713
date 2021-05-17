package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class SkuAttrValueServiceImplTest {
    @Resource
    SkuAttrValueService skuAttrValueService;
    @Test
    void test(){
        System.out.println(this.skuAttrValueService.querySaleAttrValuesMappingSkuIdBySpuId(7l));
    }

}