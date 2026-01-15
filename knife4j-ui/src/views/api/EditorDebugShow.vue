<template>
  <div class="editor-debug-show-wrapper">
    <div v-if="debugResponse">
      <editor class="knife4j-debug-ace-editor" @input="change" :options="debugOptions" v-model:value="valueText" @init="editorInit"
        :lang="mode" theme="eclipse" width="100%" :style="{height: editorHeight + 'px'}"></editor>
    </div>
    <div v-else>
      <editor v-model:value="valueText" @init="editorInit" @input="change" :lang="mode" theme="eclipse" width="100%"
        :style="{height: editorHeight + 'px'}"></editor>
    </div>

  </div>
</template>

<script>
import { VAceEditor } from 'vue3-ace-editor'
import ace from "ace-builds";
import "ace-builds/src-noconflict/mode-json.js";
import "ace-builds/src-noconflict/mode-xml.js";
import "ace-builds/src-noconflict/mode-text.js";
import "ace-builds/src-noconflict/mode-javascript.js";
import "ace-builds/src-noconflict/theme-eclipse.js";
import "ace-builds/src-noconflict/ext-language_tools";
import { ref, watch } from 'vue'
export default {
  name: "EditorShow",
  components: { editor: VAceEditor },
  props: {
    value: {
      type: String,
      required: true,
      default: ""
    },
    mode: {
      type: String,
      required: true,
      default: "json"
    },
    debugResponse: {
      type: Boolean,
      default: false
    },
    // 是否启用 AI 分析结果高亮
    enableAiHighlight: {
      type: Boolean,
      default: false
    }
  },
  emits: ['update:value', 'debugEditorChange', 'showDescription'],
  setup(props) {
    const valueText = ref(props.value)
    const markerIds = ref([])
    
    watch(() => props.value, (newVal) => {
      valueText.value = newVal
    }, { immediate: false })
    
    return {
      valueText,
      markerIds
    }
  },
  data() {
    return {
      editor: null,
      editorHeight: 200,
      debugOptions: {
        readOnly: false,
        autoScrollEditorIntoView: true,
        displayIndentGuides: false,
        fixedWidthGutter: true,
        showPrintMargin: false
      },
      commonOptions: {
        readOnly: false
      }
    };
  },
  watch: {
    // 监听内容变化，触发高亮
    valueText: {
      handler(newVal) {
        if (this.enableAiHighlight && this.editor) {
          this.$nextTick(() => {
            this.applyAiHighlight();
          });
        }
      },
      immediate: false
    }
  },
  methods: {
    resetEditorHeight() {
      var that = this;
      //  重设高度
      setTimeout(() => {
        var length_editor = that.editor.session.getLength();
        if (length_editor == 1) {
          length_editor = 15;
        }
        if (length_editor < 15) {
          if (that.debugResponse) {
            length_editor = 30;
          } else {
            length_editor = 15;
          }
        }
        if (length_editor > 20) {
          if (!that.debugResponse) {
            length_editor = 20;
          }
        }
        var rows_editor = length_editor * 16;
        if (rows_editor > 2000) {
          rows_editor = 2000;
        }
        // console.log(rows_editor)
        that.editorHeight = rows_editor;
      }, 10);
    },
    change() {
      // this.value = value;
      // 重设高度
      this.$emit("update:value", this.valueText);
      if (!this.debugResponse) {
        this.resetEditorHeight();
      }
    },
    /**
     * 应用 AI 分析结果高亮
     * - errorReason, rootError: 红色背景
     * - analysis, solution: 绿色背景（solution 数组高亮整个范围）
     */
    applyAiHighlight() {
      if (!this.editor || !this.enableAiHighlight) return;
      
      const session = this.editor.getSession();
      const content = session.getValue();
      if (!content) return;
      
      const lines = content.split('\n');
      const Range = ace.require('ace/range').Range;
      
      // 清除之前的 markers
      this.markerIds.forEach(id => {
        session.removeMarker(id);
      });
      this.markerIds = [];
      
      // 红色高亮字段
      const redFields = ['errorReason', 'rootError', 'httpStatus'];
      // 绿色高亮字段（单行）
      const greenFields = ['analysis'];
      
      let inSolutionArray = false;
      let bracketCount = 0;
      
      lines.forEach((line, index) => {
        // 检查红色字段
        for (const field of redFields) {
          if (line.includes(`"${field}"`)) {
            const markerId = session.addMarker(
              new Range(index, 0, index, 1),
              'ai-highlight-red',
              'fullLine'
            );
            this.markerIds.push(markerId);
            break;
          }
        }
        
        // 检查绿色字段（单行）
        for (const field of greenFields) {
          if (line.includes(`"${field}"`)) {
            const markerId = session.addMarker(
              new Range(index, 0, index, 1),
              'ai-highlight-green',
              'fullLine'
            );
            this.markerIds.push(markerId);
            break;
          }
        }
        
        // 处理 solution 数组 - 从 "solution" 到对应的 ] 都高亮
        if (line.includes('"solution"')) {
          inSolutionArray = true;
          bracketCount = 0;
        }
        
        if (inSolutionArray) {
          const markerId = session.addMarker(
            new Range(index, 0, index, 1),
            'ai-highlight-green',
            'fullLine'
          );
          this.markerIds.push(markerId);
          
          // 计算方括号来确定数组边界
          for (const char of line) {
            if (char === '[') bracketCount++;
            if (char === ']') bracketCount--;
          }
          
          // 当方括号平衡且遇到了 ]，结束高亮
          if (bracketCount <= 0 && line.includes(']')) {
            inSolutionArray = false;
          }
        }
      });
    },
    editorInit(editor) {
      var that = this;
      // console("aaa");
      this.editor = editor;
      // require("brace/ext/language_tools"); // language extension prerequsite...
      // require("brace/theme/eclipse");
      // require("brace/mode/json");
      // require("brace/mode/text");
      // require("brace/mode/html");
      // require("brace/mode/xml");
      // require("brace/mode/javascript");
      /*  if (this.mode == "json") {
      } else if (this.mode == "html") {
      } else if (this.mode == "text") {
      } else if (this.mode == "xml") {
      } else if (this.mode == "javascript") {
      } */
      // this.editor.gotoLine(1);
      if (this.debugResponse) {
        // 启动换行
        this.editor.getSession().setUseWrapMode(true);
        this.editor.setOptions(this.debugOptions);
        if (this.mode == "text") {
          this.editor.getSession().setUseWrapMode(true);
        }
      } else {
        this.editor.setOptions(this.commonOptions);
      }
      // 重设高度
      this.resetEditorHeight();
      this.editor.renderer.on("afterRender", function () {
        var length_editor = that.editor.session.getLength();
        that.$emit("showDescription", length_editor)
      });
      
      // 如果启用了 AI 高亮，初始化时也应用一次
      if (this.enableAiHighlight) {
        this.$nextTick(() => {
          this.applyAiHighlight();
        });
      }
    }
  }
};
</script>

<style>
/* AI 分析结果高亮样式 */
.ai-highlight-red {
  background-color: rgba(255, 77, 79, 0.2) !important;
  position: absolute;
}

.ai-highlight-green {
  background-color: rgba(82, 196, 26, 0.2) !important;
  position: absolute;
}
</style>