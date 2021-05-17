package com.atguigu.gmall.item.service;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.ItemException;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemService {
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private TemplateEngine templateEngine;

    private void createHtml(Long skuId){
        ItemVo itemVo = this.loadData(skuId);
        Context context = new Context();
        context.setVariable("itemVo",itemVo);
        try(PrintWriter printWriter = new PrintWriter(new File("D:\\内网通文件\\锋哥\\"+skuId+".html"))){

            templateEngine.process("item",context,printWriter);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }
    public void asyncExecute(Long skuId){
        threadPoolExecutor.execute(()->createHtml(skuId));
    }



    public ItemVo loadData(Long skuId) {

        ItemVo itemVo = new ItemVo();
        //sku信息
        CompletableFuture<SkuEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new ItemException("该skuid对应的商品不存在");
            }
            itemVo.setSkuId(skuId);
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;

        }, threadPoolExecutor);


        //查询分类信息
        CompletableFuture<Void> future1 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<CategoryEntity>> categoryResponseVo = this.pmsClient.queryLvl123CategoriesByCid3(skuEntity.getCatagoryId());
            List<CategoryEntity> categoryEntities = categoryResponseVo.getData();
            itemVo.setCategories(categoryEntities);

        }, threadPoolExecutor);


        //查询品牌信息
        CompletableFuture<Void> future2 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());

            }

        }, threadPoolExecutor);


        //查询spu的信息
        CompletableFuture<Void> future3 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if (spuEntity != null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());

            }

        }, threadPoolExecutor);


        //查询sku图片列表
        CompletableFuture<Void> future4 = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> listResponseVo = pmsClient.queryImagesBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntities = listResponseVo.getData();

            itemVo.setSkuImages(skuImagesEntities);

        }, threadPoolExecutor);

        //查询营销信息
        CompletableFuture<Void> future5 = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> salesResponseVo = smsClient.querySalesBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            itemVo.setSales(itemSaleVos);

        }, threadPoolExecutor);

        //查询库存信息
        CompletableFuture<Void> future6 = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {

                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));

            }

        }, threadPoolExecutor);


        //查询销售属性spu所有
        CompletableFuture<Void> future7 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValueBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrsResponseVo.getData();

            itemVo.setSaleAttrs(saleAttrValueVos);

        }, threadPoolExecutor);

        //当前sku的销售属性
        CompletableFuture<Void> future8 = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> skuAttrResponseVo = this.pmsClient.querySaleAttrValueBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                Map<Long, String> collect = skuAttrValueEntities.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));

                itemVo.setSaleAttr(collect);
            }

        }, threadPoolExecutor);


        //销售属性组合和sku的映射关系
        CompletableFuture<Void> future9 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> skuMappingResponseVo = this.pmsClient.querySaleAttrValuesMappingSkuIdBySpuId(skuEntity.getSpuId());
            String json = skuMappingResponseVo.getData();
            itemVo.setSkusJson(json);

        }, threadPoolExecutor);

        //查询商品详情spu图片列表
        CompletableFuture<Void> future10 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = this.pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if (spuDescEntity != null) {
                String[] split = StringUtils.split(spuDescEntity.getDecript(), ",");
                itemVo.setSpuImages(Arrays.asList(split));
            }


        }, threadPoolExecutor);

        //查询规格参数及组下的规格信息
        CompletableFuture<Void> future11 = skuFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<ItemGroupVo>> groupResponseVo = this.pmsClient.queryGroupsWithAttrsAndValuesByCidAndSpuIdAndSkuId(skuEntity.getCatagoryId(), skuId, skuEntity.getSpuId());
            List<ItemGroupVo> itemGroupVos = groupResponseVo.getData();
            itemVo.setGroups(itemGroupVos);


        }, threadPoolExecutor);
        CompletableFuture.allOf(future1,future2,future3,future4,future5,future6,future7,future8,future9,future10,future11).join();

        return itemVo;
    }
}










class CompletableFutureDemo{
    public static void main(String[] args) {
        CompletableFuture.runAsync(()->{
            System.out.println("runAsync方法初始化了一个任务"+Thread.currentThread().getName());
        });
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("supplyAsync方法初始化了一个任务");
            return "hello completableFuture";
        });

        CompletableFuture<String> future3 = future.thenApplyAsync(t -> {
            System.out.println("shudhus");
            System.out.println("返回结果" + t);
            return "thenApplyAsync";
        });
        CompletableFuture<Void> future2 = future.thenAccept(t -> {
            System.out.println("sjisis");
            System.out.println(t);
        });
        CompletableFuture<Void> future1 = future.thenRunAsync(() -> {
            System.out.println("dhhdfu");
        });
        CompletableFuture.allOf(future,future1,future2,future3).join();

//                .whenCompleteAsync((t,u)->{
//            System.out.println(t);
//            System.out.println(u);
//        });
        System.out.println("主线程");
    }
}
