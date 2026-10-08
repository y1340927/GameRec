module.exports = {
  lintOnSave: false,
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/images': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  publicPath: '/',
  // 生产构建优化
  productionSourceMap: false,
  // CSS 优化
  css: {
    extract: true,
    sourceMap: false
  },
  // Webpack 配置
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          // Element UI 单独打包
          elementUI: {
            name: 'chunk-element-ui',
            test: /[\\/]node_modules[\\/]element-ui[\\/]/,
            priority: 20,
            chunks: 'all'
          },
          // ECharts 单独打包
          echarts: {
            name: 'chunk-echarts',
            test: /[\\/]node_modules[\\/](echarts|zrender)[\\/]/,
            priority: 20,
            chunks: 'all'
          },
          // 其他第三方库
          vendors: {
            name: 'chunk-vendors',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'all'
          }
        }
      }
    },
    // 性能提示
    performance: {
      hints: 'warning',
      maxEntrypointSize: 512000,
      maxAssetSize: 256000
    }
  }
};
