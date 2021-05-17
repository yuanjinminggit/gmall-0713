package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.AuthException;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {
    @Autowired
    GmallUmsClient umsClient;
    @Resource
    JwtProperties jwtProperties;


    public void login(String loginName, String passWord, HttpServletRequest request, HttpServletResponse response) {
        //调用接口,查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = this.umsClient.queryUser(loginName, passWord);
        UserEntity userEntity = userEntityResponseVo.getData();


        //判断用户是否为空

        if (userEntity ==null){
            throw new AuthException("用户名或者密码错误");
        }
        try {
            //组装载荷信息,防止盗用加入用户IP地址

            HashMap<String , Object> map = new HashMap<>();
            map.put("userId",userEntity.getId());
            map.put("userName",userEntity.getUsername());
            String ip = IpUtils.getIpAddressAtService(request);
            map.put("ip",ip);

            //生成jwt类型token
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());
            //放入cookie中
            CookieUtils.setCookie(request,response,this.jwtProperties.getCookieName(),token,this.jwtProperties.getExpire()*60);
            //昵称翻入cookie中
            CookieUtils.setCookie(request,response,this.jwtProperties.getUnick(),userEntity.getNickname(),this.jwtProperties.getExpire()*60);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
