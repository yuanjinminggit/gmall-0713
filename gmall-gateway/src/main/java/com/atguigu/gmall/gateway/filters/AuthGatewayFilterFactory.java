package com.atguigu.gmall.gateway.filters;

import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {
    @Resource
    private JwtProperties jwtProperties;
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("pathes");
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();

//                System.out.println("这是一个局部过滤器");
////                System.out.println("config中的key"+config.key);
//                System.out.println("pathes"+config.pathes);
                //判断请求在不在拦截名单中
                List<String> pathes = config.pathes;
                String curPath = request.getURI().getPath();
                if (!CollectionUtils.isEmpty(pathes)){
                    if (!pathes.stream().anyMatch(path->curPath.startsWith(path))){
                        return chain.filter(exchange);
                    }
                }
                //获取请求中的token信息,同步cookie,一步头信息中获取
                //异步
                HttpHeaders headers = request.getHeaders();
                String token = headers.getFirst("token");
                //tongbu
                if (StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies)&&cookies.containsKey(jwtProperties.getCookieName())){
                        HttpCookie cookie = cookies.getFirst(jwtProperties.getCookieName());
                        //List<HttpCookie> httpCookies = cookies.get(jwtProperties.getCookieName());
                        token = cookie.getValue();

                    }
                }
                //判断token是否为空,为空重定向到登录页面
                if (StringUtils.isBlank(token)){
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
                    //拦截请求
                    return response.setComplete();
                }
                //解析token信息,如果解析出现异常,重定向到登录页面
                try {
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
                    //判断token中的IP和当前请求中的IP是否一致
                    String ip = map.get("ip").toString();
                    String curi = IpUtils.getIpAddressAtGateway(request);
                    if (!StringUtils.equals(ip,curi)){
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
                        //拦截请求
                        return response.setComplete();

                    }
                    //把解析后的登录信息产地给后续服务

                    request.mutate().header("userId",map.get("userId").toString()).build();
                    exchange.mutate().request(request).build();

                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://sso.gmall.com/toLogin.html?returnUrl="+request.getURI());
                    //拦截请求
                    return response.setComplete();
                }
                //放行
                return chain.filter(exchange);
            }
        };
    }
    @Data
    public static class PathConfig{
        private  List<String>pathes;
    }

    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }
    //    @Data
//    public static class KeyValueConfig{
//        private String key;
//        private String value;
//        private String desc;
//
//    }
}
