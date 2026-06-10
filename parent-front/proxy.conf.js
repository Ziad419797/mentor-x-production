module.exports = {
  '/api': {
    target: 'http://localhost:8081',
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
    onProxyReq: function (proxyReq) {
      proxyReq.removeHeader('Origin');
      proxyReq.removeHeader('Referer');
    }
  }
};
