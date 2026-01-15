/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.filter;

import com.github.zhanglongjun.knife4j.aggregator.conf.GlobalConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.Base64;

/**
 * WebFlux Basic认证过滤器
 */
public class WebFluxSecurityBasicAuthFilter extends AbstractBasicAuthFilter implements WebFilter {
    
    /**
     * 是否开启basic验证
     */
    private boolean enableBasicAuth = false;
    
    private String userName;
    
    private String password;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (this.enableBasicAuth && this.match(exchange.getRequest().getURI().toString())) {
            return exchange.getSession().doOnNext(session -> this.doFilter(exchange, session))
                    .then(chain.filter(exchange));
        }
        return chain.filter(exchange);
    }
    
    private void doFilter(ServerWebExchange exchange, WebSession session) {
        Object attribute = session.getAttribute(GlobalConstants.KNIFE4J_BASIC_AUTH_SESSION);
        if (attribute != null) {
            return;
        }
        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        ServerHttpResponse response = exchange.getResponse();
        if (authorization == null) {
            writeForbiddenCode(response);
        }
        
        String[] parts = authorization.split(" ");
        if (parts.length != 2 || !parts[0].equals(BASIC)) {
            writeForbiddenCode(response);
        }
        
        String credentials = new String(Base64.getDecoder().decode(parts[1]));
        String[] usernameAndPassword = credentials.split(":");
        if (usernameAndPassword.length != 2 || !usernameAndPassword[0].equals(this.userName)
                || !usernameAndPassword[1].equals(this.password)) {
            writeForbiddenCode(response);
        } else {
            exchange.getSession().doOnNext(
                    session1 -> session1.getAttributes().put(GlobalConstants.KNIFE4J_BASIC_AUTH_SESSION, this.userName))
                    .subscribe();
        }
    }
    
    private void writeForbiddenCode(ServerHttpResponse serverHttpResponse) {
        serverHttpResponse.setRawStatusCode(HttpStatus.UNAUTHORIZED.value());
        serverHttpResponse.getHeaders().add(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Restricted Area\"");
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED.value(), null, null);
    }
    
    // Getters and Setters
    
    public boolean isEnableBasicAuth() {
        return enableBasicAuth;
    }
    
    public void setEnableBasicAuth(boolean enableBasicAuth) {
        this.enableBasicAuth = enableBasicAuth;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
}

