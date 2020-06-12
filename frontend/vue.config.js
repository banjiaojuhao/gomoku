module.exports = {
  devServer: {
    port: '8001',
    proxy: {
      '/eventbus': {
        target: 'http://111.231.251.93:8080/', // API 服务器的地址
        ws: true,
        changeOrigin: true,
      }
    }
  },
}