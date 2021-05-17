package com.atguigu.gmall.pms.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SkuMapperTest {
    @Autowired
    SpuMapper spuMapper;
    @Test
    public void t(){
        System.out.println(this.spuMapper.selectById(10));
    }

}