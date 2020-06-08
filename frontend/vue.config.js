module.exports = {
  devServer: {
    port: '8001',
    proxy: {
      '/api': {
        target: 'http://localhost:8000', // API 服务器的地址
        changeOrigin: true,
      }
    }
  },
}