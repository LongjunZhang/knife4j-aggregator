<template>
  <a-layout-content class="knife4j-body-content diff-page">
    <!-- 顶部工具栏 -->
    <div class="diff-toolbar">
      <div class="toolbar-left">
        <span class="service-name">{{ serviceName }}</span>
        <a-divider type="vertical" />
        <span class="diff-title">版本对比</span>
      </div>
      <div class="toolbar-right">
        <a-space>
          <span>旧版本:</span>
          <a-select
            v-model:value="v1"
            style="width: 120px"
            :loading="loading"
            placeholder="选择版本"
          >
            <a-select-option
              v-for="v in versionList"
              :key="v.version"
              :value="v.version"
              :disabled="v.version === v2"
            >
              v{{ v.version }}
            </a-select-option>
          </a-select>
          
          <span>新版本:</span>
          <a-select
            v-model:value="v2"
            style="width: 120px"
            :loading="loading"
            placeholder="选择版本"
          >
            <a-select-option
              v-for="v in versionList"
              :key="v.version"
              :value="v.version"
              :disabled="v.version === v1"
            >
              v{{ v.version }}
              <a-tag v-if="v.version === latestVersion" color="blue" size="small">最新</a-tag>
            </a-select-option>
          </a-select>
          
          <a-button type="primary" @click="loadDiff" :loading="diffLoading">
            <template #icon><swap-outlined /></template>
            对比
          </a-button>
        </a-space>
      </div>
    </div>
    
    <!-- 摘要卡片 -->
    <div class="diff-summary" v-if="diffResult">
      <a-card size="small">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="变更摘要" :value="diffResult.summary || '无变更'" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="新增接口" :value="diffResult.addedCount || 0" value-style="color: #52c41a">
              <template #prefix><plus-circle-outlined /></template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="删除接口" :value="diffResult.removedCount || 0" value-style="color: #ff4d4f">
              <template #prefix><minus-circle-outlined /></template>
            </a-statistic>
          </a-col>
          <a-col :span="6">
            <a-statistic title="修改接口" :value="diffResult.modifiedCount || 0" value-style="color: #faad14">
              <template #prefix><edit-outlined /></template>
            </a-statistic>
          </a-col>
        </a-row>
      </a-card>
    </div>
    
    <!-- 变更类型过滤 -->
    <div class="diff-filter" v-if="diffResult && diffResult.changes && diffResult.changes.length > 0">
      <a-radio-group v-model:value="filterType" button-style="solid">
        <a-radio-button value="ALL">全部 ({{ diffResult.changes.length }})</a-radio-button>
        <a-radio-button value="ADDED">新增 ({{ diffResult.addedCount }})</a-radio-button>
        <a-radio-button value="REMOVED">删除 ({{ diffResult.removedCount }})</a-radio-button>
        <a-radio-button value="MODIFIED">修改 ({{ diffResult.modifiedCount }})</a-radio-button>
      </a-radio-group>
    </div>
    
    <!-- 变更列表 -->
    <div class="diff-changes" v-if="diffResult">
      <a-collapse v-model:activeKey="activeKeys" v-if="filteredChanges.length > 0">
        <a-collapse-panel 
          v-for="(change, index) in filteredChanges" 
          :key="index"
          :header="getChangeHeader(change)"
        >
          <template #extra>
            <a-tag :color="getChangeTypeColor(change.changeType)">
              {{ getChangeTypeLabel(change.changeType) }}
            </a-tag>
          </template>
          
          <!-- 左右对比面板（使用高亮组件） -->
          <DiffViewer
            :old-content="change.oldValue"
            :new-content="change.newValue"
            :left-title="`旧版本 (v${v1})`"
            :right-title="`新版本 (v${v2})`"
            :show-header="false"
            mode="side-by-side"
          />
        </a-collapse-panel>
      </a-collapse>
      
      <a-empty v-else-if="!diffLoading" description="没有变更" />
    </div>
    
    <!-- 初始状态 -->
    <div class="diff-empty" v-if="!diffResult && !diffLoading">
      <a-empty description="请选择两个版本进行对比">
        <template #image>
          <diff-outlined style="font-size: 64px; color: #d9d9d9;" />
        </template>
      </a-empty>
    </div>
    
    <!-- 加载状态 -->
    <div class="diff-loading" v-if="diffLoading">
      <a-spin size="large" tip="正在对比版本..." />
    </div>
  </a-layout-content>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useGlobalsStore } from '@/store/modules/global.js'
import { getVersions, diffVersions, parseServiceName } from '@/services/docAggregatorApi.js'
import { message } from 'ant-design-vue'
import {
  SwapOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  EditOutlined,
  DiffOutlined
} from '@ant-design/icons-vue'
import DiffViewer from '@/components/DiffViewer/index.vue'

export default {
  name: 'DiffPage',
  components: {
    SwapOutlined,
    PlusCircleOutlined,
    MinusCircleOutlined,
    EditOutlined,
    DiffOutlined,
    DiffViewer
  },
  setup() {
    const route = useRoute()
    const globalsStore = useGlobalsStore()
    
    // 状态
    const serviceName = ref('')
    const versionList = ref([])
    const v1 = ref(null)
    const v2 = ref(null)
    const loading = ref(false)
    const diffLoading = ref(false)
    const diffResult = ref(null)
    const filterType = ref('ALL')
    const activeKeys = ref([])
    
    // 计算属性
    const latestVersion = computed(() => {
      if (versionList.value.length > 0) {
        return versionList.value[0].version
      }
      return null
    })
    
    const filteredChanges = computed(() => {
      if (!diffResult.value || !diffResult.value.changes) {
        return []
      }
      if (filterType.value === 'ALL') {
        return diffResult.value.changes
      }
      return diffResult.value.changes.filter(c => c.changeType === filterType.value)
    })
    
    /**
     * 初始化服务名
     */
    function initServiceName() {
      // 从路由参数获取
      const groupName = route.params.groupName
      if (groupName) {
        serviceName.value = groupName
        return
      }
      
      // 从 store 获取
      if (globalsStore.currentServiceName) {
        serviceName.value = globalsStore.currentServiceName
        return
      }
      
      // 从 swaggerCurrentInstance 解析
      const instance = globalsStore.swaggerCurrentInstance
      if (instance && instance.url) {
        serviceName.value = parseServiceName(instance.url)
      }
    }
    
    /**
     * 加载版本列表
     */
    async function loadVersionList() {
      if (!serviceName.value) {
        return
      }
      
      loading.value = true
      try {
        const versions = await getVersions(serviceName.value)
        versionList.value = versions
        
        // 默认选择最新两个版本
        if (versions.length >= 2) {
          v2.value = versions[0].version  // 最新版本
          v1.value = versions[1].version  // 次新版本
        } else if (versions.length === 1) {
          v2.value = versions[0].version
        }
      } catch (error) {
        console.error('加载版本列表失败', error)
        message.error('加载版本列表失败')
      } finally {
        loading.value = false
      }
    }
    
    /**
     * 加载 Diff 结果
     */
    async function loadDiff() {
      if (!serviceName.value || !v1.value || !v2.value) {
        message.warning('请选择两个版本进行对比')
        return
      }
      
      if (v1.value === v2.value) {
        message.warning('两个版本不能相同')
        return
      }
      
      diffLoading.value = true
      diffResult.value = null
      activeKeys.value = []
      
      try {
        const result = await diffVersions(serviceName.value, v1.value, v2.value)
        if (result.success) {
          diffResult.value = result
          // 默认展开前3个变更
          if (result.changes && result.changes.length > 0) {
            activeKeys.value = result.changes.slice(0, 3).map((_, i) => i)
          }
        } else {
          message.error(result.error || '对比失败')
        }
      } catch (error) {
        console.error('版本对比失败', error)
        message.error('版本对比失败')
      } finally {
        diffLoading.value = false
      }
    }
    
    /**
     * 获取变更类型颜色
     */
    function getChangeTypeColor(type) {
      switch (type) {
        case 'ADDED': return 'green'
        case 'REMOVED': return 'red'
        case 'MODIFIED': return 'orange'
        default: return 'default'
      }
    }
    
    /**
     * 获取变更类型标签
     */
    function getChangeTypeLabel(type) {
      switch (type) {
        case 'ADDED': return '新增'
        case 'REMOVED': return '删除'
        case 'MODIFIED': return '修改'
        default: return type
      }
    }
    
    /**
     * 获取变更标题
     */
    function getChangeHeader(change) {
      const method = change.method ? change.method.toUpperCase() : ''
      return `${method} ${change.path}`
    }
    
    /**
     * 格式化 JSON
     */
    function formatJson(jsonStr) {
      if (!jsonStr) return ''
      try {
        const obj = typeof jsonStr === 'string' ? JSON.parse(jsonStr) : jsonStr
        return JSON.stringify(obj, null, 2)
      } catch (e) {
        return jsonStr
      }
    }
    
    // 监听路由参数变化
    watch(() => route.params.groupName, (newVal) => {
      if (newVal) {
        serviceName.value = newVal
        loadVersionList()
      }
    })
    
    // 组件挂载
    onMounted(() => {
      initServiceName()
      loadVersionList()
    })
    
    return {
      serviceName,
      versionList,
      v1,
      v2,
      loading,
      diffLoading,
      diffResult,
      filterType,
      activeKeys,
      latestVersion,
      filteredChanges,
      loadDiff,
      getChangeTypeColor,
      getChangeTypeLabel,
      getChangeHeader,
      formatJson
    }
  }
}
</script>

<style scoped lang="less">
.diff-page {
  padding: 16px;
  background: #f5f5f5;
  min-height: calc(100vh - 120px);
}

.diff-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
  margin-bottom: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  
  .toolbar-left {
    display: flex;
    align-items: center;
    
    .service-name {
      font-size: 16px;
      font-weight: 600;
      color: #1890ff;
    }
    
    .diff-title {
      font-size: 14px;
      color: #666;
    }
  }
}

.diff-summary {
  margin-bottom: 16px;
}

.diff-filter {
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 4px;
}

.diff-changes {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
}

// DiffViewer 组件样式由组件自身提供

.diff-empty,
.diff-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
  background: #fff;
  border-radius: 4px;
}
</style>

