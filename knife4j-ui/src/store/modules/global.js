import { defineStore } from 'pinia'

export const useGlobalsStore = defineStore('Globals',{
  state() {
    return {
      menuData: [],
      language: 'zh-CN',
      swagger: null,
      swaggerCurrentInstance: null,
      enableVersion: false,
      enableAfterScript: true,
      enableResponseCode: true,
      enableReloadCacheParameter: false,
      currentMenuData: [],
      serviceOptions: [],
      settings: {},
      defaultServiceOption: '',
      loading: {
        show: false,
        text: '加载中...'
      },
      // ========== 版本管理相关状态 ==========
      // 当前服务的版本列表
      versionList: [],
      // 当前选中的版本号
      currentVersion: null,
      // 最新版本号（用于标识）
      latestVersion: null,
      // 当前服务名（从 swaggerCurrentInstance.url 解析）
      currentServiceName: null,
      // 版本文档内容（用于渲染指定版本）
      versionDocContent: null,
      // 是否正在加载版本
      versionLoading: false,
      // 是否启用版本功能（聚合服务模式下启用）
      enableVersionFeature: true,
      // ========== Tab 管理相关 ==========
      // 清空 Tab 的回调函数（由 BasicLayout 注册）
      clearTabsCallback: null
    }
  },
  actions: {
    setSettings(settings) {
      this.settings = settings;
    },
    setReloadCacheParameter(reloadCacheParameter) {
      this.enableReloadCacheParameter = reloadCacheParameter;
    },
    setAfterScript(afterScript) {
      this.enableAfterScript = afterScript;
    },
    setResponseCode(enableResponseCode) {
      this.enableResponseCode = enableResponseCode;
    },
    setGitVersion(gitVersion) {
      this.enableVersion = gitVersion;
    },
    setMenuData(menudatas) {
      this.menuData = this.menuData.concat(menudatas);
      this.currentMenuData = menudatas;
    },
    setCurrentMenuData(menudatas) {
      this.currentMenuData = menudatas;
    },
    /**
     * 清空当前服务的菜单数据（版本切换时使用）
     * 保留其他服务的菜单数据，只清空当前服务的
     */
    clearCurrentServiceMenuData() {
      // 获取当前服务的 groupId
      const currentGroupId = this.swaggerCurrentInstance?.groupId;
      if (currentGroupId) {
        // 过滤掉当前服务的菜单项
        this.menuData = this.menuData.filter(item => item.groupId !== currentGroupId);
      }
      this.currentMenuData = [];
    },
    setLang(lang) {
      this.language = lang;
    },
    setSwagger(swagger) {
      this.swagger = swagger;
    },
    setSwaggerInstance(instance) {
      this.swaggerCurrentInstance = instance;
    },
    setServiceOptions(services) {
      this.serviceOptions = services;
    },
    setDefaultService(defaultOption) {
      this.defaultServiceOption = defaultOption;
    },
    showLoading(options) {
      this.loading.show = true
      if (options) {
        this.loading.text = options.text
      }
    },
    destroyLoading() {
      this.loading.show = false
      this.loading.text = '加载中...'
    },
    
    // ========== 版本管理相关 actions ==========
    
    /**
     * 设置版本列表
     * 自动将最新版本设为默认选中
     */
    setVersionList(versions) {
      this.versionList = versions || [];
      // 设置最新版本号（列表第一个，按版本号倒序）
      if (this.versionList.length > 0) {
        this.latestVersion = this.versionList[0].version;
        // 如果当前没有选中版本，默认选中最新版本
        if (this.currentVersion === null) {
          this.currentVersion = this.latestVersion;
        }
      } else {
        this.latestVersion = null;
        this.currentVersion = null;
      }
    },
    
    /**
     * 设置当前选中的版本
     */
    setCurrentVersion(version) {
      this.currentVersion = version;
    },
    
    /**
     * 设置当前服务名
     */
    setCurrentServiceName(serviceName) {
      this.currentServiceName = serviceName;
      // 切换服务时重置版本状态
      this.versionList = [];
      this.currentVersion = null;
      this.latestVersion = null;
      this.versionDocContent = null;
    },
    
    /**
     * 设置版本文档内容
     */
    setVersionDocContent(content) {
      this.versionDocContent = content;
    },
    
    /**
     * 设置版本加载状态
     */
    setVersionLoading(loading) {
      this.versionLoading = loading;
    },
    
    /**
     * 设置是否启用版本功能
     */
    setEnableVersionFeature(enable) {
      this.enableVersionFeature = enable;
    },
    
    /**
     * 重置版本状态（切换服务时调用）
     */
    resetVersionState() {
      this.versionList = [];
      this.currentVersion = null;
      this.latestVersion = null;
      this.versionDocContent = null;
      this.versionLoading = false;
    },
    
    // ========== Tab 管理相关 actions ==========
    
    /**
     * 注册清空 Tab 的回调函数（由 BasicLayout 调用）
     */
    registerClearTabsCallback(callback) {
      this.clearTabsCallback = callback;
    },
    
    /**
     * 清空所有 Tab 页面（保留主页）
     * 版本切换时调用
     */
    clearAllTabs() {
      if (typeof this.clearTabsCallback === 'function') {
        this.clearTabsCallback();
      }
    }
  }
})
