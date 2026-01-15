import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import viteCompression from 'vite-plugin-compression';
import removeConsole from 'vite-plugin-remove-console';
import { resolve } from 'path'
import { nodePolyfills } from 'vite-plugin-node-polyfills'

// https://vitejs.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    vue(),
    vueJsx(),
    Components({
      resolvers: [AntDesignVueResolver()]
    }),
    nodePolyfills(),
    viteCompression({
      deleteOriginFile: false, //删除源文件
      threshold: 10240, //压缩前最小文件大小
      algorithm: 'gzip', //压缩算法
      ext: '.gz', //文件类型
    }),
    // removeConsole()
  ],
  resolve: {
    alias: [
      { find: '@', replacement: resolve(__dirname, 'src') },
      { find: /^~/, replacement: '' },
    ]
  },
  // 开启less支持
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true
      }
    }
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      // =====================================================
      // 重要说明：
      // 前端通过 /v3/api-docs/swagger-config 获取服务列表和文档路径，
      // 所有服务的 Swagger JSON 路径由 swagger-config 返回值动态决定，
      // 不在此处硬编码具体服务路径！
      // =====================================================
      
      // swagger-config 端点（固定路径）
      '/v3/api-docs/swagger-config': {
        target: 'http://localhost:9090',
        changeOrigin: true
      },
      // 管理 API（版本管理、缓存管理等）
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true
      },
      // 通用规则：匹配所有服务的 OpenAPI 文档路径
      // 格式：/{contextPath}/v2/api-docs 或 /{contextPath}/v3/api-docs
      // 这是动态路径，由 swagger-config 返回值决定
      '^/[\\w-]+/v[23]/api-docs': {
        target: 'http://localhost:9090',
        changeOrigin: true
      },
      // 通用规则：匹配所有服务的业务 API 请求（用于 Try it out）
      // 格式：/{contextPath}/xxx 
      // Gateway 会根据路由配置将请求转发到对应的微服务
      '^/[\\w-]+/': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        // 排除静态资源路径
        bypass: (req) => {
          // 如果是静态资源请求，返回 false 表示不代理
          const staticPaths = ['/src/', '/node_modules/', '/@', '/webjars/'];
          if (staticPaths.some(p => req.url.startsWith(p))) {
            return req.url;
          }
          // 如果是 HTML/CSS/JS 等静态文件，不代理
          if (/\.(html|css|js|jsx|ts|tsx|vue|json|ico|png|jpg|svg|woff|ttf)$/.test(req.url)) {
            return req.url;
          }
          return null; // 继续代理
        }
      }
    }
  },
  build: {
    rollupOptions: {
      input: 'doc.html',
      output: {
        chunkFileNames: 'webjars/js/[name]-[hash].js',
        entryFileNames: 'webjars/js/[name]-[hash].js',
        assetFileNames: 'webjars/[ext]/[name]-[hash].[ext]'
      }
    }
  }
})
