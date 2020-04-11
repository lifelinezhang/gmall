package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {
    private static final String pubKeyPath = "G:\\gmall\\rsa\\rsa.pub";

    private static final String priKeyPath = "G:\\gmall\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234!@#%gsdf^%");
    }

    @Before
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
        String token = JwtUtils.generateToken(map, privateKey, 1);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token =
                "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE1ODY1NzM1ODV9.LPt_ktUKghOeIEKEJek21d07w_RkPFGC__vLKTXerzO0WgkXD1s5sWCReb_guk_v5J2qFkI05RPb4ATdBU35EBW-Swglj8rICdzqFhrM4HNdZ1KEY3sOrOOw0N8W0tpGS4VP8NuYCDUo0qakLZq5VHuoTZhNQtYlq44ItJjCQfEDTMT1O6GFPBlslBgxWt1Xh2BxHKIciQiGNrZH6yxzjgy00ESi09i1upAJ8PymSSgbROLU72ubioxlJtVa-D_sMiEhm23_VPCjr8uhi8vLSN488yAhunHF12tXj99zmQh0XvoaY1kuZu2sxon3hTUqhyXfkpDqaEGZIHo2o1O0sA";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}