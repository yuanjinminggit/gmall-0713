package com.atguigu.gmall.wms.vo;

import io.swagger.models.auth.In;
import lombok.Data;

@Data
public class SkuLockVo {
    private Long skuId;
    private Integer count;
    private Boolean lock;
    private Long wareSkuId;//记录库存ID
}
