package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
	private static final String pubKeyPath = "D:\\内网通文件\\锋哥\\project-0713\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\内网通文件\\锋哥\\project-0713\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MDk2NjE3MDF9.FiwuZq_Dtlr8zzB6lcFGu6RflxDgIKAF8wWiN0Hog2bgkP5drRCFqxtiwtrloLtbSXHtJ08h4dY19RsKShsOziR1w4Leg19vooZwec1oOu7-TtHmFunsBp_DOLj_3QK1_KrfRJcalhFc-rlKiW5rasSLsetEWMmGrbQYaGKPh4KYFh7I0gswQDcNQ6I4on8u3XogVPsZ4xItLFVrzF_eq8FhGa4vVM5vsPfpAsMFGHJ_9_xrVgj1_H1aKSJq6cdDkrv8FzY8_3SjZxcCdCZyZxDxf0nFyCUfe-eogXf0xIdi9PxWWH9vVuVdPS-yNvLXBIsb3UxFagxpO2pnO_f5_w";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}