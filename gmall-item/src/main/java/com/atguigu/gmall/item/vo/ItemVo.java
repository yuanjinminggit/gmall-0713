package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {
    private List<CategoryEntity> categories;


    private Long brandId;
    private String brandName;


    private Long spuId;
    private String spuName;

    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private String defaultImage;
    private Integer weight;



    private List<SkuImagesEntity>skuImages;



    private List<ItemSaleVo>sales;



    private  Boolean store = false;

//[{attrId:4,attrName:颜色,attrValues:[黑,白]},{},{}]销售属性
    private List<SaleAttrValueVo> saleAttrs;


//{4:黑,5:4g,6:128g}选中的销售属性
    private Map<Long,String> saleAttr;


//当前销售属性和skuid的映射关系
//    {'白色,8g,128g':100,''},
    private String skusJson;



    private List<String> spuImages;




    private List<ItemGroupVo>groups;
}
