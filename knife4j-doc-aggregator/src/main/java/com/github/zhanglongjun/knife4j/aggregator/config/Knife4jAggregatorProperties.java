/*
 * Knife4j Aggregator - API 文档聚合服务
 */
package com.github.zhanglongjun.knife4j.aggregator.config;

import com.github.zhanglongjun.knife4j.aggregator.conf.GlobalConstants;
import com.github.zhanglongjun.knife4j.aggregator.enums.AggregatorStrategy;
import com.github.zhanglongjun.knife4j.aggregator.enums.GroupOrderStrategy;
import com.github.zhanglongjun.knife4j.aggregator.enums.OpenApiVersion;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.*;

/**
 * Knife4j 聚合器配置属性
 * 
 * 合并原来的 Knife4jGatewayProperties 和 Knife4jAutoDiscoveryConfig
 * 使用新的配置前缀 knife4j.aggregator.*
 */
@ConfigurationProperties(prefix = "knife4j.aggregator")
public class Knife4jAggregatorProperties {
    
    /**
     * 是否启用聚合OpenAPI规范文档聚合
     */
    private boolean enabled = false;
    
    /**
     * Enable HTTP Basic authentication
     */
    private Basic basic;
    
    /**
     * 文档聚合策略,默认服务发现
     */
    private AggregatorStrategy strategy = AggregatorStrategy.DISCOVER;
    
    /**
     * tag排序规则
     */
    private GroupOrderStrategy tagsSorter = GroupOrderStrategy.alpha;
    
    /**
     * operation接口排序规则
     */
    private GroupOrderStrategy operationsSorter = GroupOrderStrategy.alpha;
    
    /**
     * 服务发现模式配置
     */
    private final Discover discover = new Discover();
    
    /**
     * 缓存配置
     */
    private final Cache cache = new Cache();
    
    /**
     * 聚合服务路由配置(manual模式或作为discover模式的补充)
     */
    private final List<Router> routes = new ArrayList<>();
    
    // Getters and Setters
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public Basic getBasic() {
        return basic;
    }
    
    public void setBasic(Basic basic) {
        this.basic = basic;
    }
    
    public AggregatorStrategy getStrategy() {
        return strategy;
    }
    
    public void setStrategy(AggregatorStrategy strategy) {
        this.strategy = strategy;
    }
    
    public GroupOrderStrategy getTagsSorter() {
        return tagsSorter;
    }
    
    public void setTagsSorter(GroupOrderStrategy tagsSorter) {
        this.tagsSorter = tagsSorter;
    }
    
    public GroupOrderStrategy getOperationsSorter() {
        return operationsSorter;
    }
    
    public void setOperationsSorter(GroupOrderStrategy operationsSorter) {
        this.operationsSorter = operationsSorter;
    }
    
    public Discover getDiscover() {
        return discover;
    }
    
    public Cache getCache() {
        return cache;
    }
    
    public List<Router> getRoutes() {
        return routes;
    }
    
    /**
     * 服务发现策略配置（合并原来的 Discover 和 AutoDiscoveryConfig）
     */
    public static class Discover {
        
        /**
         * 是否开启服务发现
         */
        private Boolean enabled = Boolean.TRUE;
        
        /**
         * 需要排除的服务名称(不区分大小写)
         */
        private Set<String> excludedServices = new HashSet<>();
        
        /**
         * 当前规范版本，默认OpenAPI3
         */
        private OpenApiVersion version = OpenApiVersion.OpenAPI3;
        
        /**
         * API 文档路径，默认为 OpenAPI 3 的路径
         */
        private String docPath = GlobalConstants.DEFAULT_OPEN_API_V3_PATH;
        
        /**
         * 服务名显示格式：original（原始名）、capitalize（首字母大写）
         */
        private String displayNameFormat = "original";
        
        /**
         * 服务 context-path 映射
         * key: 服务名（如 user-service）
         * value: context-path（如 /userService）
         */
        private Map<String, String> serviceContextPaths = new HashMap<>();
        
        /**
         * 是否启用端点自动探测
         * 启用后，会自动探测 /v3/api-docs 和 /v2/api-docs
         */
        private boolean autoProbeEndpoint = true;
        
        /**
         * 端点探测超时时间（毫秒）
         */
        private long probeTimeout = 3000L;
        
        /**
         * 服务端点映射（手动覆盖自动探测结果）
         * key: 服务名（如 message-service）
         * value: 文档端点路径（如 /v2/api-docs）
         */
        private Map<String, String> serviceEndpoints = new HashMap<>();
        
        /**
         * 针对OpenAPI3规范的个性化配置
         */
        private final OpenApiV3 oas3 = new OpenApiV3();
        
        /**
         * 针对Swagger2规范的个性化配置
         */
        private final OpenApiV2 swagger2 = new OpenApiV2();
        
        /**
         * 各个子服务个性化配置
         */
        private final Map<String, ServiceConfigInfo> serviceConfig = new HashMap<>();
        
        // Getters and Setters
        
        public Boolean getEnabled() {
            return enabled;
        }
        
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
        
        public Set<String> getExcludedServices() {
            return excludedServices;
        }
        
        public void setExcludedServices(Set<String> excludedServices) {
            this.excludedServices = excludedServices;
        }
        
        public OpenApiVersion getVersion() {
            return version;
        }
        
        public void setVersion(OpenApiVersion version) {
            this.version = version;
        }
        
        public String getDocPath() {
            return docPath;
        }
        
        public void setDocPath(String docPath) {
            this.docPath = docPath;
        }
        
        public String getDisplayNameFormat() {
            return displayNameFormat;
        }
        
        public void setDisplayNameFormat(String displayNameFormat) {
            this.displayNameFormat = displayNameFormat;
        }
        
        public Map<String, String> getServiceContextPaths() {
            return serviceContextPaths;
        }
        
        public void setServiceContextPaths(Map<String, String> serviceContextPaths) {
            this.serviceContextPaths = serviceContextPaths;
        }
        
        public boolean isAutoProbeEndpoint() {
            return autoProbeEndpoint;
        }
        
        public void setAutoProbeEndpoint(boolean autoProbeEndpoint) {
            this.autoProbeEndpoint = autoProbeEndpoint;
        }
        
        public long getProbeTimeout() {
            return probeTimeout;
        }
        
        public void setProbeTimeout(long probeTimeout) {
            this.probeTimeout = probeTimeout;
        }
        
        public Map<String, String> getServiceEndpoints() {
            return serviceEndpoints;
        }
        
        public void setServiceEndpoints(Map<String, String> serviceEndpoints) {
            this.serviceEndpoints = serviceEndpoints;
        }
        
        /**
         * 获取指定服务的文档端点路径
         * @param serviceName 服务名
         * @return 文档端点路径，如果未配置则返回 null
         */
        public String getServiceEndpoint(String serviceName) {
            return serviceEndpoints.get(serviceName);
        }
        
        public OpenApiV3 getOas3() {
            return oas3;
        }
        
        public OpenApiV2 getSwagger2() {
            return swagger2;
        }
        
        public Map<String, ServiceConfigInfo> getServiceConfig() {
            return serviceConfig;
        }
        
        /**
         * 获取当前服务的URL
         */
        public String getUrl() {
            if (this.version == OpenApiVersion.OpenAPI3) {
                return this.oas3.getUrl();
            }
            if (this.version == OpenApiVersion.Swagger2) {
                return this.swagger2.getUrl();
            }
            return GlobalConstants.DEFAULT_OPEN_API_V2_PATH;
        }
        
        /**
         * 获取指定服务的 context-path
         * @param serviceName 服务名
         * @return context-path，如果未配置则返回空字符串
         */
        public String getContextPath(String serviceName) {
            return serviceContextPaths.getOrDefault(serviceName, "");
        }
    }
    
    /**
     * 缓存配置
     */
    public static class Cache {
        
        /**
         * 是否启用缓存
         */
        private boolean enabled = true;
        
        /**
         * 硬过期时间（毫秒），默认 24 小时
         */
        private long hardTtl = 86400000L;
        
        /**
         * 启动时是否预热缓存
         */
        private boolean warmUpOnStartup = true;
        
        /**
         * 拉取文档的超时时间（毫秒），默认 10 秒
         */
        private long fetchTimeout = 10000L;
        
        // Getters and Setters
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public long getHardTtl() {
            return hardTtl;
        }
        
        public void setHardTtl(long hardTtl) {
            this.hardTtl = hardTtl;
        }
        
        public boolean isWarmUpOnStartup() {
            return warmUpOnStartup;
        }
        
        public void setWarmUpOnStartup(boolean warmUpOnStartup) {
            this.warmUpOnStartup = warmUpOnStartup;
        }
        
        public long getFetchTimeout() {
            return fetchTimeout;
        }
        
        public void setFetchTimeout(long fetchTimeout) {
            this.fetchTimeout = fetchTimeout;
        }
    }
    
    /**
     * HTTP Basic 认证配置
     */
    public static class Basic {
        
        /**
         * 是否启用HTTP basic认证
         */
        private boolean enable = false;
        
        /**
         * HTTP basic 用户名
         */
        private String username;
        
        /**
         * HTTP basic 密码
         */
        private String password;
        
        /**
         * 需要验证的URL正则表达式列表
         */
        private List<String> include;
        
        // Getters and Setters
        
        public boolean isEnable() {
            return enable;
        }
        
        public void setEnable(boolean enable) {
            this.enable = enable;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
        
        public List<String> getInclude() {
            return include;
        }
        
        public void setInclude(List<String> include) {
            this.include = include;
        }
    }
    
    /**
     * 服务个性化配置
     */
    public static class ServiceConfigInfo {
        
        /**
         * 当前服务排序
         */
        private Integer order = GlobalConstants.DEFAULT_ORDER;
        
        /**
         * 当前服务的分组名称
         */
        private String groupName;
        
        /**
         * 组名称集合
         */
        private List<String> groupNames;
        
        /**
         * contextPath
         */
        private String contextPath;
        
        // Getters and Setters
        
        public Integer getOrder() {
            return order;
        }
        
        public void setOrder(Integer order) {
            this.order = order;
        }
        
        public String getGroupName() {
            return groupName;
        }
        
        public void setGroupName(String groupName) {
            this.groupName = groupName;
        }
        
        public List<String> getGroupNames() {
            return groupNames;
        }
        
        public void setGroupNames(List<String> groupNames) {
            this.groupNames = groupNames;
        }
        
        public String getContextPath() {
            return contextPath;
        }
        
        public void setContextPath(String contextPath) {
            this.contextPath = contextPath;
        }
    }
    
    /**
     * 自定义接口路由
     */
    public static class Router {
        
        /**
         * 分组名称
         */
        private String name;
        
        /**
         * 服务名称(Optional)
         */
        private String serviceName;
        
        /**
         * OpenAPI数据源加载url地址
         */
        private String url = GlobalConstants.DEFAULT_OPEN_API_V2_PATH;
        
        /**
         * contextPath
         */
        private String contextPath = GlobalConstants.DEFAULT_API_PATH_PREFIX;
        
        /**
         * 排序(asc),默认不排序
         */
        private Integer order = GlobalConstants.DEFAULT_ORDER;
        
        // Getters and Setters
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getServiceName() {
            return serviceName;
        }
        
        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
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
        
        public Integer getOrder() {
            return order;
        }
        
        public void setOrder(Integer order) {
            this.order = order;
        }
    }
    
    /**
     * Swagger2规范的个性化配置
     */
    public static class OpenApiV2 {
        
        /**
         * OpenAPI数据源加载url地址
         */
        private String url = GlobalConstants.DEFAULT_OPEN_API_V2_PATH;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    /**
     * OpenAPI3规范的个性化配置
     */
    public static class OpenApiV3 {
        
        /**
         * OpenAPI数据源加载url地址
         */
        private String url = GlobalConstants.DEFAULT_OPEN_API_V3_PATH;
        
        /**
         * OAuth2重定向地址
         */
        private String oauth2RedirectUrl = "";
        
        /**
         * validatorUrl
         */
        private String validatorUrl = "";
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getOauth2RedirectUrl() {
            return oauth2RedirectUrl;
        }
        
        public void setOauth2RedirectUrl(String oauth2RedirectUrl) {
            this.oauth2RedirectUrl = oauth2RedirectUrl;
        }
        
        public String getValidatorUrl() {
            return validatorUrl;
        }
        
        public void setValidatorUrl(String validatorUrl) {
            this.validatorUrl = validatorUrl;
        }
    }
}

