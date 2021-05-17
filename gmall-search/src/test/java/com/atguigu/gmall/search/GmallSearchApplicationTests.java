package com.atguigu.gmall.search;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.search.feign.GmallPmsClient;
import com.atguigu.gmall.search.feign.GmallWmsClient;
import com.atguigu.gmall.search.pojo.Goods;
import com.atguigu.gmall.search.pojo.SearchAttrValueVo;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class GmallSearchApplicationTests {
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Resource
    private GoodsRepository goodsRepository;
    @Resource
    private GmallPmsClient pmsClient;
    @Resource
    private GmallWmsClient wmsClient;
    @Test
    void contextLoads() {
//        this.elasticsearchRestTemplate.createIndex(Goods.class);
//        this.elasticsearchRestTemplate.putMapping(Goods.class);
        Integer pageNum = 1;
        Integer pageSize = 100;
    do {
        PageParamVo pageParamVo = new PageParamVo();
        pageParamVo.setPageNum(pageNum);
        pageParamVo.setPageSize(pageSize);
        ResponseVo<List<SpuEntity>> responseVo = this.pmsClient.querySpuByPageJson(pageParamVo);
        List<SpuEntity> spuEntities = responseVo.getData();
        if (CollectionUtils.isEmpty(spuEntities)){
            break;
        }
        spuEntities.forEach(spuEntity -> {
            ResponseVo<List<SkuEntity>> skuResponseVo = this.pmsClient.querySkusBySpuId(spuEntity.getId());
            List<SkuEntity> skuEntities = skuResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuEntities)){
                List<Goods> goodsList = skuEntities.stream().map(skuEntity ->{
                            Goods goods = new Goods();


                            goods.setSkuId(skuEntity.getId());
                            goods.setTitle(skuEntity.getTitle());
                            goods.setSubTitle(skuEntity.getSubtitle());
                            goods.setDefaultImage(skuEntity.getDefaultImage());
                            goods.setPrice(skuEntity.getPrice().doubleValue());


                            goods.setCreateTime(spuEntity.getCreateTime());


                            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(skuEntity.getId());
                            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
                            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                                goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a,b)->a+b).get());
                                goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
                            }



                            ResponseVo<BrandEntity> brandEntityResponseVo = this.pmsClient.queryBrandById(skuEntity.getBrandId());
                            BrandEntity brandEntity = brandEntityResponseVo.getData();
                            if (brandEntity!=null){
                                goods.setBrandId(brandEntity.getId());
                                goods.setBrandName(brandEntity.getName());
                                goods.setLogo(brandEntity.getLogo());
                            }



                            ResponseVo<CategoryEntity> categoryEntityResponseVo = this.pmsClient.queryCategoryById(skuEntity.getCatagoryId());
                            CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
                            if (categoryEntity!=null){
                                goods.setCategoryId(categoryEntity.getId());
                                goods.setCategoryName(categoryEntity.getName());
                            }


                            List<SearchAttrValueVo> searchAttrValueVos = new ArrayList<>();


                            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = this.pmsClient.querySearchSkuAttrValuesByCidAndSkuId(skuEntity.getCatagoryId(), skuEntity.getId());
                            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
                            if (!CollectionUtils.isEmpty(skuAttrValueEntities)){
                                searchAttrValueVos.addAll( skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                                    SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                    BeanUtils.copyProperties(skuAttrValueEntity,searchAttrValueVo);
                                    return searchAttrValueVo;
                                }).collect(Collectors.toList()));

                            }


                            ResponseVo<List<SpuAttrValueEntity>> spuAttrValueResponseVo = this.pmsClient.querySearchSpuAttrValuesByCidAndSpuId(skuEntity.getCatagoryId(), spuEntity.getId());
                            List<SpuAttrValueEntity> spuAttrValueEntities = spuAttrValueResponseVo.getData();
                            if (!CollectionUtils.isEmpty(spuAttrValueEntities)){
                                searchAttrValueVos.addAll(spuAttrValueEntities.stream().map(spuAttrValueEntity -> {
                                    SearchAttrValueVo searchAttrValueVo = new SearchAttrValueVo();
                                    BeanUtils.copyProperties(spuAttrValueEntity,searchAttrValueVo);
                                    return searchAttrValueVo;
                                }).collect(Collectors.toList()));
                            }

                            goods.setSearchAttrs(searchAttrValueVos);


                            return goods;
                        }
                        ).collect(Collectors.toList());


                this.goodsRepository.saveAll(goodsList);

            }
        });





        pageSize++;
        pageSize = spuEntities.size();
    }while (pageSize==100);










    }

}
