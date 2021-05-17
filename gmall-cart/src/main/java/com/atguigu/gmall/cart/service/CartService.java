package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginIntercepter;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.concurrent.ListenableFuture;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    private static final String key_prefix="cart:info:";
    private static final String price_prefix="cart:price:";
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private CartMapper cartMapper;
    @Autowired
    private CartAsyncService cartAsyncService;

    public void addCart(Cart cart) {
        //获取登录状态
        String userId = getUserId();
        String key=key_prefix+userId;
        //根据外层的key过去内层的map
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        String skuId = cart.getSkuId().toString();
        BigDecimal count = cart.getCount();
        if (hashOps.hasKey(skuId)){
            String json = hashOps.get(skuId).toString();
            cart = JSON.parseObject(json,Cart.class);
            cart.setCount(cart.getCount().add(count));
            hashOps.put(skuId,JSON.toJSONString(cart));
            cartAsyncService.updateCartByUserIdAndSkuId(userId,cart,skuId);
        }else{
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity==null){
                throw new CartException("没有对应商品");
            }
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setDefaultImage(skuEntity.getDefaultImage());
            cart.setUserId(userId);
            cart.setCheck(true);
            ResponseVo<List<SkuAttrValueEntity>> saleAttrsResponseVo = this.pmsClient.querySaleAttrValueBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValues = saleAttrsResponseVo.getData();

            cart.setSaleAttrs(JSON.toJSONString(skuAttrValues));
            ResponseVo<List<ItemSaleVo>> salesResponseVo = this.smsClient.querySalesBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();
            cart.setSales(JSON.toJSONString(itemSaleVos));
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.wmsClient.queryWareSkusBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)){
                cart.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));

            }
            hashOps.put(skuId,JSON.toJSONString(cart));
            cartAsyncService.insertCart(userId,cart);
            this.redisTemplate.opsForValue().set(price_prefix+skuId,cart.getPrice().toString());

        }


    }


    private String getUserId() {
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        String userId = userInfo.getUserKey();
        if(userInfo.getUserId()!=null){
            userId= userInfo.getUserId().toString();
        }
        return userId;
    }

    public Cart queryCartBySkuId(Long skuId) {
        String userId = this.getUserId();
        String key = key_prefix + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (!hashOps.hasKey(skuId.toString())){
            throw new CartException("没有对应的购物车记录");
        }
        String json = hashOps.get(skuId.toString()).toString();

        if (StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,Cart.class);

        }
        throw new CartException("没有对应的购物车记录");





    }
    @Async
    public String  execute1() throws Exception {

            System.out.println("这是execute1方法开始执行");
            TimeUnit.SECONDS.sleep(5);
            int i = 1/0;
            System.out.println("这是execute1方法结束执行");

        return "execuete1";

    }
    @Async
    public ListenableFuture<String> execute2(){
        try {
            System.out.println("这是execute2方法开始执行");
            TimeUnit.SECONDS.sleep(3);
            System.out.println("这是execute2方法结束执行");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return AsyncResult.forValue("execute2");

    }

    public List<Cart> queryCarts() {
        UserInfo userInfo = LoginIntercepter.getUserInfo();
        String userKey = userInfo.getUserKey();

        //先查未登录的购物车
        String unLoginKey = key_prefix + userKey;
        BoundHashOperations<String, Object, Object> unLoginHashOps = this.redisTemplate.boundHashOps(unLoginKey);
        List<Object> unLoginCartJsons = unLoginHashOps.values();
        List<Cart> unLoginCarts=null;
        if (!CollectionUtils.isEmpty(unLoginCartJsons)){
            unLoginCarts = unLoginCartJsons.stream().map(cartJson ->{
                   Cart cart =  JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                   return cart;
                   }).collect(Collectors.toList());

        }


        //获取登录状态,未登录则直接返回未登录的购物车
        Long userId = userInfo.getUserId();
        if (userId==null){
            return unLoginCarts;
        }
        //登录则合并
        String loginKey = key_prefix +userId;
        BoundHashOperations<String, Object, Object> loginHashOps = this.redisTemplate.boundHashOps(loginKey);
        if (!CollectionUtils.isEmpty(unLoginCartJsons)){
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();
                BigDecimal count = cart.getCount();
                if (loginHashOps.hasKey(skuId)){
                    //更新数量
                    String cartJson = loginHashOps.get(skuId).toString();
                     cart = JSON.parseObject(cartJson, Cart.class);
                     cart.setCount(count.add(cart.getCount()));
                     loginHashOps.put(skuId,JSON.toJSONString(cart));
                     cartAsyncService.updateCartByUserIdAndSkuId(userId.toString(),cart,skuId);
                }else{
                    //新增一条记录
                    cart.setUserId(userId.toString());
                    loginHashOps.put(skuId,JSON.toJSONString(cart));
                    this.cartAsyncService.insertCart(userId.toString(),cart);
                }
            });
        }

        //删除未登录的购物车
          //同步删除redis中的购物车
        redisTemplate.delete(unLoginKey);
        //一步删除数据库中的购物车
        this.cartAsyncService.deleteByUserId(userKey);
        //查询登录状态的购物车
        List<Object> loginCartJsons = loginHashOps.values();
        if (CollectionUtils.isEmpty(loginCartJsons)){
            return null;
        }
        return loginCartJsons.stream().map(cartJson->{

            Cart cart = JSON.parseObject(cartJson.toString(),Cart.class);

            cart.setCurrentPrice(new BigDecimal(redisTemplate.opsForValue().get(price_prefix + cart.getSkuId())));

            return cart;
        }).collect(Collectors.toList());


    }

    public void updateNum(Cart cart) {
        String userId = this.getUserId();
        String key = key_prefix + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        if (!hashOps.hasKey(cart.getSkuId().toString())){
            throw new CartException("该用户没有对应的购物车记录");

        }
        //用户要更新的数量
        BigDecimal count = cart.getCount();
        String json = hashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(json,Cart.class);
        //更新redis中商品数量
        cart.setCount(count);
        hashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        this.cartAsyncService.updateCartByUserIdAndSkuId(userId,cart,cart.getSkuId().toString());
    }

    public void deleteCart(Long skuId) {
        String userId = this.getUserId();
        String key = key_prefix + userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        hashOps.delete(skuId.toString());
        this.cartAsyncService.deleteCart(userId,skuId);


        
    }

    public List<Cart> queryCheckedCardByUserId(Long userId) {
        String key = key_prefix+userId;
        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        List<Object> cartjsons = hashOps.values();
        if (CollectionUtils.isEmpty(cartjsons)){
            throw new CartException("该用户没有购物车记录");

        }

        return cartjsons.stream().map(cartjon->JSON.parseObject(cartjon.toString(),Cart.class)).filter(Cart::getCheck).collect(Collectors.toList());
    }
}
