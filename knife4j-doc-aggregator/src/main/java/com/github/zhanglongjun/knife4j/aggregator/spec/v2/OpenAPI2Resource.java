/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.spec.v2;

import com.github.zhanglongjun.knife4j.aggregator.config.Knife4jAggregatorProperties;
import com.github.zhanglongjun.knife4j.aggregator.spec.AbstractOpenAPIResource;
import com.github.zhanglongjun.knife4j.aggregator.utils.PathUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * OpenAPI 2.x / Swagger 资源
 */
public class OpenAPI2Resource extends AbstractOpenAPIResource {
    
    /**
     * 服务发现场景下的服务名称
     */
    private transient String serviceName;
    
    private String name;
    private String url;
    private String contextPath;
    private String id;
    
    /**
     * 获取 servicePath（与 contextPath 相同，用于兼容 knife4j-vue3 前端）
     * @return servicePath 值（等同于 contextPath）
     */
    public String getServicePath() {
        return this.contextPath;
    }
    
    public OpenAPI2Resource(Integer order, Boolean discovered) {
        super(order, discovered);
    }
    
    /**
     * 基于Router配置对象构建接口Resource
     * @param router Config配置对象
     */
    public OpenAPI2Resource(Knife4jAggregatorProperties.Router router) {
        super(router.getOrder(), false);
        this.name = router.getName();
        this.url = router.getUrl();
        this.contextPath = router.getContextPath();
        this.id = Base64.getEncoder().encodeToString((router.getName() + router.getUrl() +
                router.getContextPath()).getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 基于参数配置构建Resource对象
     */
    public OpenAPI2Resource(String url,
                            int order,
                            boolean discover,
                            String groupName,
                            String contextPath) {
        super(order, discover);
        this.name = groupName;
        this.url = url;
        this.contextPath = PathUtils.processContextPath(contextPath);
        this.id = Base64.getEncoder().encodeToString((groupName + url +
                contextPath).getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 增加服务名称
     */
    public OpenAPI2Resource(String url,
                            int order,
                            boolean discover,
                            String groupName,
                            String contextPath, 
                            String serviceName) {
        this(url, order, discover, groupName, contextPath);
        this.serviceName = serviceName;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenAPI2Resource that = (OpenAPI2Resource) o;
        return Objects.equals(getName(), that.getName()) 
                && Objects.equals(getUrl(), that.getUrl()) 
                && Objects.equals(getContextPath(), that.getContextPath()) 
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getServiceName(), that.getServiceName());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getName(), getUrl(), getContextPath(), getId(), getServiceName());
    }
    
    /**
     * 复制一个新对象
     * @return resource对象实例
     */
    public OpenAPI2Resource copy() {
        return new OpenAPI2Resource(this.url, this.order, this.discovered, this.name, this.contextPath, this.serviceName);
    }
    
    @Override
    public String toString() {
        return "OpenAPI2Resource{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", contextPath='" + contextPath + '\'' +
                ", id='" + id + '\'' +
                ", order=" + order +
                ", discovered=" + discovered +
                ", serviceName=" + serviceName +
                '}';
    }
}

