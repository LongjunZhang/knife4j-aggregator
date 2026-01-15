<template>
  <div class="diff-viewer">
    <div class="diff-header" v-if="showHeader">
      <span class="diff-title">{{ title }}</span>
      <span class="diff-stats" v-if="stats">
        <span class="stat-added" v-if="stats.added > 0">+{{ stats.added }}</span>
        <span class="stat-removed" v-if="stats.removed > 0">-{{ stats.removed }}</span>
      </span>
    </div>
    
    <div class="diff-content" :class="{ 'side-by-side': mode === 'side-by-side', 'unified': mode === 'unified' }">
      <!-- 左右对比模式 -->
      <template v-if="mode === 'side-by-side'">
        <div class="diff-side diff-left">
          <div class="side-header">
            <span>{{ leftTitle }}</span>
          </div>
          <div class="side-content">
            <div 
              v-for="(line, index) in leftLines" 
              :key="'left-' + index"
              class="diff-line"
              :class="getLineClass(line, 'left')"
            >
              <span class="line-number">{{ index + 1 }}</span>
              <span class="line-content" v-html="highlightLine(line.content, line.type)"></span>
            </div>
            <div v-if="leftLines.length === 0" class="empty-placeholder">
              无内容
            </div>
          </div>
        </div>
        
        <div class="diff-side diff-right">
          <div class="side-header">
            <span>{{ rightTitle }}</span>
          </div>
          <div class="side-content">
            <div 
              v-for="(line, index) in rightLines" 
              :key="'right-' + index"
              class="diff-line"
              :class="getLineClass(line, 'right')"
            >
              <span class="line-number">{{ index + 1 }}</span>
              <span class="line-content" v-html="highlightLine(line.content, line.type)"></span>
            </div>
            <div v-if="rightLines.length === 0" class="empty-placeholder">
              无内容
            </div>
          </div>
        </div>
      </template>
      
      <!-- 统一视图模式 -->
      <template v-else>
        <div class="unified-content">
          <div 
            v-for="(line, index) in unifiedLines" 
            :key="'unified-' + index"
            class="diff-line"
            :class="getUnifiedLineClass(line)"
          >
            <span class="line-prefix">{{ getLinePrefix(line) }}</span>
            <span class="line-content" v-html="highlightLine(line.content, line.type)"></span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { computed, ref } from 'vue'

export default {
  name: 'DiffViewer',
  props: {
    // 旧内容（JSON 字符串或对象）
    oldContent: {
      type: [String, Object],
      default: null
    },
    // 新内容（JSON 字符串或对象）
    newContent: {
      type: [String, Object],
      default: null
    },
    // 显示模式：side-by-side | unified
    mode: {
      type: String,
      default: 'side-by-side'
    },
    // 是否显示头部
    showHeader: {
      type: Boolean,
      default: true
    },
    // 标题
    title: {
      type: String,
      default: '变更对比'
    },
    // 左侧标题
    leftTitle: {
      type: String,
      default: '旧版本'
    },
    // 右侧标题
    rightTitle: {
      type: String,
      default: '新版本'
    }
  },
  setup(props) {
    /**
     * 格式化 JSON 为字符串
     */
    function formatJson(content) {
      if (!content) return ''
      try {
        const obj = typeof content === 'string' ? JSON.parse(content) : content
        return JSON.stringify(obj, null, 2)
      } catch (e) {
        return typeof content === 'string' ? content : JSON.stringify(content)
      }
    }
    
    /**
     * 将字符串按行拆分
     */
    function splitLines(str) {
      if (!str) return []
      return str.split('\n')
    }
    
    /**
     * 简单的行级别 diff
     */
    function computeDiff(oldStr, newStr) {
      const oldLines = splitLines(formatJson(oldStr))
      const newLines = splitLines(formatJson(newStr))
      
      const leftResult = []
      const rightResult = []
      const unifiedResult = []
      
      // 使用简单的逐行比较（实际项目中可使用更复杂的 diff 算法）
      const maxLen = Math.max(oldLines.length, newLines.length)
      
      let addedCount = 0
      let removedCount = 0
      
      for (let i = 0; i < maxLen; i++) {
        const oldLine = oldLines[i] !== undefined ? oldLines[i] : null
        const newLine = newLines[i] !== undefined ? newLines[i] : null
        
        if (oldLine === newLine) {
          // 相同行
          leftResult.push({ content: oldLine || '', type: 'unchanged' })
          rightResult.push({ content: newLine || '', type: 'unchanged' })
          unifiedResult.push({ content: oldLine || '', type: 'unchanged' })
        } else if (oldLine === null) {
          // 新增行
          leftResult.push({ content: '', type: 'placeholder' })
          rightResult.push({ content: newLine, type: 'added' })
          unifiedResult.push({ content: newLine, type: 'added' })
          addedCount++
        } else if (newLine === null) {
          // 删除行
          leftResult.push({ content: oldLine, type: 'removed' })
          rightResult.push({ content: '', type: 'placeholder' })
          unifiedResult.push({ content: oldLine, type: 'removed' })
          removedCount++
        } else {
          // 修改行
          leftResult.push({ content: oldLine, type: 'removed' })
          rightResult.push({ content: newLine, type: 'added' })
          unifiedResult.push({ content: oldLine, type: 'removed' })
          unifiedResult.push({ content: newLine, type: 'added' })
          addedCount++
          removedCount++
        }
      }
      
      return {
        left: leftResult,
        right: rightResult,
        unified: unifiedResult,
        stats: { added: addedCount, removed: removedCount }
      }
    }
    
    // 计算 diff 结果
    const diffResult = computed(() => {
      return computeDiff(props.oldContent, props.newContent)
    })
    
    const leftLines = computed(() => diffResult.value.left)
    const rightLines = computed(() => diffResult.value.right)
    const unifiedLines = computed(() => diffResult.value.unified)
    const stats = computed(() => diffResult.value.stats)
    
    /**
     * 获取行的 CSS 类
     */
    function getLineClass(line, side) {
      const classes = []
      if (line.type === 'added') classes.push('line-added')
      if (line.type === 'removed') classes.push('line-removed')
      if (line.type === 'placeholder') classes.push('line-placeholder')
      return classes
    }
    
    /**
     * 获取统一视图行的 CSS 类
     */
    function getUnifiedLineClass(line) {
      if (line.type === 'added') return 'line-added'
      if (line.type === 'removed') return 'line-removed'
      return ''
    }
    
    /**
     * 获取行前缀符号
     */
    function getLinePrefix(line) {
      if (line.type === 'added') return '+'
      if (line.type === 'removed') return '-'
      return ' '
    }
    
    /**
     * 高亮行内容
     */
    function highlightLine(content, type) {
      // 简单的语法高亮
      if (!content) return ''
      
      let highlighted = content
        // 转义 HTML
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        // 高亮字符串
        .replace(/"([^"\\]*(\\.[^"\\]*)*)"/g, '<span class="hl-string">"$1"</span>')
        // 高亮数字
        .replace(/\b(\d+)\b/g, '<span class="hl-number">$1</span>')
        // 高亮布尔和 null
        .replace(/\b(true|false|null)\b/g, '<span class="hl-keyword">$1</span>')
      
      return highlighted
    }
    
    return {
      leftLines,
      rightLines,
      unifiedLines,
      stats,
      getLineClass,
      getUnifiedLineClass,
      getLinePrefix,
      highlightLine
    }
  }
}
</script>

<style scoped lang="less">
.diff-viewer {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
}

.diff-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  
  .diff-title {
    font-weight: 500;
    color: #333;
  }
  
  .diff-stats {
    .stat-added {
      color: #52c41a;
      margin-right: 8px;
    }
    
    .stat-removed {
      color: #ff4d4f;
    }
  }
}

.diff-content {
  &.side-by-side {
    display: flex;
    
    .diff-side {
      flex: 1;
      overflow: auto;
      
      &.diff-left {
        border-right: 1px solid #e8e8e8;
        
        .side-header {
          background: #fff1f0;
          border-bottom-color: #ffccc7;
        }
      }
      
      &.diff-right {
        .side-header {
          background: #f6ffed;
          border-bottom-color: #b7eb8f;
        }
      }
      
      .side-header {
        padding: 6px 12px;
        background: #fafafa;
        border-bottom: 1px solid #e8e8e8;
        font-weight: 500;
        color: #666;
      }
      
      .side-content {
        max-height: 400px;
        overflow: auto;
      }
    }
  }
  
  &.unified {
    .unified-content {
      max-height: 500px;
      overflow: auto;
    }
  }
}

.diff-line {
  display: flex;
  line-height: 1.6;
  
  .line-number {
    flex-shrink: 0;
    width: 40px;
    padding: 0 8px;
    text-align: right;
    color: #999;
    background: #fafafa;
    border-right: 1px solid #e8e8e8;
    user-select: none;
  }
  
  .line-prefix {
    flex-shrink: 0;
    width: 20px;
    text-align: center;
    font-weight: bold;
  }
  
  .line-content {
    flex: 1;
    padding: 0 8px;
    white-space: pre;
    overflow-x: auto;
  }
  
  &.line-added {
    background: #f6ffed;
    
    .line-prefix {
      color: #52c41a;
    }
  }
  
  &.line-removed {
    background: #fff1f0;
    
    .line-prefix {
      color: #ff4d4f;
    }
  }
  
  &.line-placeholder {
    background: #f5f5f5;
    
    .line-content {
      color: #ccc;
    }
  }
}

.empty-placeholder {
  padding: 24px;
  text-align: center;
  color: #999;
}

// 语法高亮
:deep(.hl-string) {
  color: #c41a16;
}

:deep(.hl-number) {
  color: #1c00cf;
}

:deep(.hl-keyword) {
  color: #aa0d91;
}
</style>

