/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.spec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * OpenAPI 资源抽象基类
 */
public abstract class AbstractOpenAPIResource implements Comparable<AbstractOpenAPIResource>, Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @JsonIgnore
    protected final Integer order;
    
    @JsonIgnore
    protected final transient Boolean discovered;
    
    protected AbstractOpenAPIResource(Integer order, Boolean discovered) {
        this.order = order;
        this.discovered = discovered;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public Boolean getDiscovered() {
        return discovered;
    }
    
    @Override
    public int compareTo(@NonNull AbstractOpenAPIResource swaggerResource) {
        int sort = this.order.compareTo(swaggerResource.getOrder());
        if (sort != 0) {
            return sort;
        }
        return this.getName().compareTo(swaggerResource.getName());
    }
    
    public abstract String getName();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractOpenAPIResource that = (AbstractOpenAPIResource) o;
        return Objects.equals(order, that.order) && Objects.equals(discovered, that.discovered);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(order, discovered);
    }
}

