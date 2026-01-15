<template>
  <div class="version-selector" v-if="enableVersionFeature">
    <!-- 使用 Dropdown 实现悬浮展示 -->
    <a-dropdown :trigger="['hover']" placement="bottomRight">
      <span class="version-trigger">
        <span class="version-text">{{ currentVersionText }}</span>
        <caret-down-outlined class="version-arrow" />
      </span>
      <template #overlay>
        <a-menu @click="handleMenuClick" class="version-menu">
          <a-menu-item 
            v-for="v in displayVersionList" 
            :key="v.version"
            :class="{ 'version-item-active': v.version === selectedVersion }"
          >
            <span class="version-item-text">{{ v.label }}</span>
          </a-menu-item>
          <!-- 如果没有版本数据，显示当前版本 -->
          <a-menu-item v-if="displayVersionList.length === 0" key="latest" disabled>
            <span class="version-item-text">latest</span>
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
  </div>
</template>

<script>
import { computed, watch, onMounted } from 'vue'
import { useGlobalsStore } from '@/store/modules/global.js'
import { getVersions, getDocByVersion, parseServiceName } from '@/services/docAggregatorApi.js'
import { CaretDownOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

export default {
  name: 'VersionSelector',
  components: {
    CaretDownOutlined
  },
  emits: ['versionChange'],
  setup(props, { emit }) {
    const globalsStore = useGlobalsStore()
    
    // 计算属性
    const enableVersionFeature = computed(() => globalsStore.enableVersionFeature)
    const versionList = computed(() => globalsStore.versionList)
    const latestVersion = computed(() => globalsStore.latestVersion)
    const versionLoading = computed(() => globalsStore.versionLoading)
    const swaggerCurrentInstance = computed(() => globalsStore.swaggerCurrentInstance)
    const swagger = computed(() => globalsStore.swagger)
    
    // 当前选中的版本
    const selectedVersion = computed(() => globalsStore.currentVersion)
    
    // 当前版本显示文本
    const currentVersionText = computed(() => {
      // 如果没有选中版本，显示 latest
      if (selectedVersion.value === null) {
        return 'latest'
      }
      // 格式化版本号（支持整数和语义化版本字符串）
      const versionStr = String(selectedVersion.value)
      const formattedVersion = `V ${versionStr}`
      // 如果是最新版本，追加 (latest) 标识
      if (selectedVersion.value === latestVersion.value) {
        return `${formattedVersion} (latest)`
      }
      return formattedVersion
    })
    
    // 显示用的版本列表（格式化）
    const displayVersionList = computed(() => {
      if (!versionList.value || versionList.value.length === 0) {
        return []
      }
      return versionList.value.map(v => ({
        version: v.version,
        label: `V ${v.version}${v.version === latestVersion.value ? ' (latest)' : ''}`
      }))
    })
    
    /**
     * 加载当前服务的版本列表
     */
    async function loadVersions() {
      if (!swaggerCurrentInstance.value?.url) {
        return
      }
      
      const serviceName = parseServiceName(swaggerCurrentInstance.value.url)
      if (!serviceName) {
        return
      }
      
      // 如果服务名没变，不重新加载
      if (serviceName === globalsStore.currentServiceName && versionList.value.length > 0) {
        return
      }
      
      globalsStore.setCurrentServiceName(serviceName)
      globalsStore.setVersionLoading(true)
      
      try {
        const versions = await getVersions(serviceName)
        globalsStore.setVersionList(versions)
      } catch (error) {
        console.error('加载版本列表失败', error)
        globalsStore.setVersionList([])
      } finally {
        globalsStore.setVersionLoading(false)
      }
    }
    
    /**
     * 切换版本并刷新文档
     */
    async function switchToVersion(version) {
      const serviceName = globalsStore.currentServiceName
      if (!serviceName) return
      
      globalsStore.setVersionLoading(true)
      
      try {
        // ★ 关键1：切换前清空所有已打开的 Tab 页面，防止用户调试旧版本接口
        globalsStore.clearAllTabs()
        
        // ★ 关键2：切换前清空当前服务的菜单数据，防止接口叠加
        globalsStore.clearCurrentServiceMenuData()
        
        // 清空 swagger 实例上的全局菜单数据（按 groupId 过滤）
        if (swagger.value && swaggerCurrentInstance.value) {
          const currentGroupId = swaggerCurrentInstance.value.groupId || swaggerCurrentInstance.value.id
          if (currentGroupId && swagger.value.globalMenuDatas) {
            swagger.value.globalMenuDatas = swagger.value.globalMenuDatas.filter(
              item => item.groupId !== currentGroupId
            )
          }
        }
        
        // 重置当前实例的内部状态
        if (swaggerCurrentInstance.value) {
          // 重置加载标记
          swaggerCurrentInstance.value.load = false
          // 清空已解析的 tags 和 paths
          swaggerCurrentInstance.value.tags = []
          swaggerCurrentInstance.value.paths = []
        }
        
        // 如果是最新版本，直接使用原始 URL 刷新
        if (version === latestVersion.value) {
          // 触发重新加载
          if (swagger.value && swaggerCurrentInstance.value) {
            swagger.value.analysisApi(swaggerCurrentInstance.value)
          }
          message.success(`已切换到最新版本 V ${version}`)
        } else {
          // 获取指定版本的文档内容
          const versionDoc = await getDocByVersion(serviceName, version)
          
          if (versionDoc && versionDoc.content) {
            // 解析文档内容
            const docContent = typeof versionDoc.content === 'string' 
              ? JSON.parse(versionDoc.content) 
              : versionDoc.content
            
            // 使用 swagger 实例的 analysisApiSuccess 方法重新渲染
            if (swagger.value) {
              // 调用文档解析成功的方法，重新渲染 UI
              swagger.value.analysisApiSuccess(docContent)
              message.success(`已切换到版本 V ${version}`)
            }
          } else {
            message.error('获取版本文档失败：内容为空')
          }
        }
        
        globalsStore.setCurrentVersion(version)
        emit('versionChange', version)
        
      } catch (error) {
        console.error('切换版本失败', error)
        message.error(`切换版本失败: ${error.message || '未知错误'}`)
      } finally {
        globalsStore.setVersionLoading(false)
      }
    }
    
    /**
     * 菜单点击处理
     * key 现在是语义化版本字符串，如 "1.0.0"
     */
    function handleMenuClick({ key }) {
      if (key === 'latest') return
      // 版本号现在是字符串，直接使用
      const version = String(key)
      // 如果点击当前已选中的版本，不做任何操作
      if (version === selectedVersion.value) return
      // 切换版本并刷新文档
      switchToVersion(version)
    }
    
    // 监听服务切换，重新加载版本列表
    watch(
      () => swaggerCurrentInstance.value?.url,
      (newUrl) => {
        if (newUrl) {
          loadVersions()
        }
      },
      { immediate: false }
    )
    
    // 组件挂载时加载版本
    onMounted(() => {
      loadVersions()
    })
    
    return {
      enableVersionFeature,
      versionList,
      latestVersion,
      versionLoading,
      selectedVersion,
      currentVersionText,
      displayVersionList,
      handleMenuClick
    }
  }
}
</script>

<style scoped>
.version-selector {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.version-trigger {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  padding: 0 4px;
}

.version-text {
  color: #011428;
  font-size: 16px;
  font-weight: 500;
  transition: color 0.2s;
}

.version-trigger:hover .version-text {
  color: #011428;
}

.version-arrow {
  color: #011428;
  font-size: 14px;
  margin-left: 4px;
  transition: color 0.2s;
}

.version-trigger:hover .version-arrow {
  color: #011428;
}

.version-menu {
  min-width: 120px;
  border-radius: 6px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
}

.version-item-text {
  font-size: 14px;
}

.version-item-active {
  background-color: #f0f7ff;
}
</style>

<style>
/* 全局样式覆盖 dropdown menu */
.version-menu .ant-dropdown-menu-item {
  padding: 8px 16px;
}

.version-menu .ant-dropdown-menu-item:hover {
  background-color: #f5f5f5;
}
</style>
