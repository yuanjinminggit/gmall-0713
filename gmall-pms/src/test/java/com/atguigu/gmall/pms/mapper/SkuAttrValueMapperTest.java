package com.atguigu.gmall.pms.mapper;

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class SkuAttrValueMapperTest {
    @Resource
    private SkuAttrValueMapper skuAttrValueMapper;
    @Test
    void test(){
        System.out.println(JSON.toJSONString(this.skuAttrValueMapper.querySaleAttrValuesMappingSkuIdBySpuId(7l)));
    }


}