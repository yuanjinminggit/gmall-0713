package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginIntercepter;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import io.swagger.annotations.Scope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

//@Scope("prototype")
@Controller
public class CartController {
    @Autowired
    private CartService cartService;
    @GetMapping
    public String addCart(Cart cart){
        this.cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addToCart.html?skuId="+cart.getSkuId();
    }

    @GetMapping("addToCart.html")
    public String addToCart(@RequestParam("skuId") Long skuId, Model model){
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart",cart);
        return "addCart";
    }





    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request) throws Exception {
        System.out.println(LoginIntercepter.getUserInfo());
        long now = System.currentTimeMillis();
        String  future1 = this.cartService.execute1();
        ListenableFuture<String> future2 = this.cartService.execute2();

//       System.out.println(future2.get());
//        future1.addCallback(t-> System.out.println("获取到子任务返回结果集"+t),ex-> System.out.println("获取到异常结果集"+ex.getMessage()));
//        future2.addCallback(t-> System.out.println("获取到子任务返回结果集"+t),ex-> System.out.println("获取到异常结果集"+ex.getMessage()));
        System.out.println(System.currentTimeMillis()-now);

        return "hello Async";

    }
    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts",carts);
        return "cart";
    }
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }
    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }
    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>>queryCheckedCartsByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheckedCardByUserId(userId);
        return ResponseVo.ok(carts);
    }
}
