<template>
  <a-drawer
    title="AI 错误分析"
    :open="visible"
    :width="480"
    @close="$emit('close')"
    class="ai-drawer"
  >
    <!-- 实时输出（始终可见） -->
    <div class="ai-section">
      <div class="ai-section-title">
        <span class="ai-icon">🧾</span>
        实时输出
      </div>
      <div class="ai-section-content">
        <a-textarea
          :rows="10"
          :value="streamText || ''"
          readonly
        />
        <div v-if="streaming" class="ai-streaming-tip">
          <a-spin size="small" /> <span>正在接收 AI 输出...</span>
        </div>
        <div v-else-if="streamError" class="ai-stream-error">
          {{ streamError }}
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="ai-drawer-loading">
      <a-spin size="large" />
      <p>AI 正在分析错误原因...</p>
    </div>
    
    <!-- 分析结果 -->
    <div v-else-if="analysis" class="ai-drawer-content">
      <!-- 错误类型 -->
      <div class="ai-section">
        <div class="ai-section-title">
          <span class="ai-icon error">✗</span>
          错误类型
        </div>
        <div class="ai-section-content">
          <a-tag color="error">{{ analysis.errorType || '未知错误' }}</a-tag>
        </div>
      </div>
      
      <!-- 根因分析 -->
      <div class="ai-section">
        <div class="ai-section-title">
          <span class="ai-icon">🔍</span>
          根因分析
        </div>
        <div class="ai-section-content">
          {{ analysis.rootCause || '无法确定根因' }}
        </div>
      </div>
      
      <!-- 修复建议 -->
      <div class="ai-section">
        <div class="ai-section-title">
          <span class="ai-icon">💡</span>
          修复建议
        </div>
        <div class="ai-section-content">
          {{ analysis.suggestion || '暂无建议' }}
        </div>
      </div>
      
      <!-- 置信度 -->
      <div class="ai-section">
        <div class="ai-section-title">
          <span class="ai-icon">📊</span>
          分析置信度
        </div>
        <div class="ai-section-content">
          <a-progress 
            :percent="Math.round((analysis.confidence || 0) * 100)" 
            :status="getConfidenceStatus(analysis.confidence)"
            :strokeColor="getConfidenceColor(analysis.confidence)"
          />
          <span class="confidence-text">
            {{ getConfidenceText(analysis.confidence) }}
          </span>
        </div>
      </div>
      
      <!-- 排查步骤 -->
      <div v-if="analysis.nextSteps && analysis.nextSteps.length > 0" class="ai-section">
        <div class="ai-section-title">
          <span class="ai-icon">📋</span>
          排查步骤
        </div>
        <div class="ai-section-content">
          <a-timeline>
            <a-timeline-item v-for="(step, index) in analysis.nextSteps" :key="index">
              {{ step }}
            </a-timeline-item>
          </a-timeline>
        </div>
      </div>
      
      <!-- 相关接口 -->
      <div v-if="analysis.relatedApis && analysis.relatedApis.length > 0" class="ai-section">
        <div class="ai-section-title">
          <span class="ai-icon">🔗</span>
          相关接口
        </div>
        <div class="ai-section-content">
          <div v-for="(api, index) in analysis.relatedApis" :key="index" class="related-api">
            <a-tag :color="getMethodColor(api.method)">{{ api.method }}</a-tag>
            <span class="api-path">{{ api.path }}</span>
            <span v-if="api.description" class="api-desc">- {{ api.description }}</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 空状态 -->
    <div v-else class="ai-drawer-empty">
      <a-empty description="暂无分析结果" />
    </div>
  </a-drawer>
</template>

<script>
export default {
  name: 'AiDrawer',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    loading: {
      type: Boolean,
      default: false
    },
    analysis: {
      type: Object,
      default: null
    },
    streamText: {
      type: String,
      default: ''
    },
    streaming: {
      type: Boolean,
      default: false
    },
    streamError: {
      type: String,
      default: null
    }
  },
  emits: ['close'],
  watch: {
    visible(val) {
      // 可以在这里添加抽屉打开/关闭时的逻辑
    },
    streamText(val) {
      // 可以在这里添加流式文本更新时的逻辑
    }
  },
  mounted() {
    // 组件挂载完成
  },
  methods: {
    getConfidenceStatus(confidence) {
      if (confidence >= 0.8) return 'success';
      if (confidence >= 0.5) return 'normal';
      return 'exception';
    },
    getConfidenceColor(confidence) {
      if (confidence >= 0.8) return '#52c41a';
      if (confidence >= 0.5) return '#1890ff';
      return '#ff4d4f';
    },
    getConfidenceText(confidence) {
      const percent = Math.round((confidence || 0) * 100);
      if (percent >= 80) return '高置信度，建议可直接执行';
      if (percent >= 50) return '中等置信度，建议进一步验证';
      return '低置信度，需要更多上下文信息';
    },
    getMethodColor(method) {
      const colors = {
        'GET': 'green',
        'POST': 'blue',
        'PUT': 'orange',
        'DELETE': 'red',
        'PATCH': 'purple'
      };
      return colors[method?.toUpperCase()] || 'default';
    }
  }
};
</script>

<style scoped>
.ai-drawer-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #666;
}

.ai-drawer-loading p {
  margin-top: 16px;
  font-size: 14px;
}

.ai-drawer-content {
  padding: 0 4px;
}

.ai-section {
  margin-bottom: 24px;
}

.ai-section-title {
  display: flex;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.ai-icon {
  margin-right: 8px;
  font-size: 16px;
}

.ai-icon.error {
  color: #ff4d4f;
}

.ai-section-content {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
  padding-left: 24px;
}

.ai-streaming-tip {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #888;
}

.ai-stream-error {
  margin-top: 10px;
  font-size: 12px;
  color: #ff4d4f;
  word-break: break-word;
}

.confidence-text {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: #888;
}

.related-api {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.api-path {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 13px;
  margin-left: 8px;
  color: #333;
}

.api-desc {
  color: #888;
  font-size: 12px;
  margin-left: 8px;
}

.ai-drawer-empty {
  padding: 60px 20px;
}
</style>





