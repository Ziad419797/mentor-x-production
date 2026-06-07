module.exports = {
  '/api': {
    target: 'http://localhost:8081',
    secure: false,
    changeOrigin: true,
    logLevel: 'info',
    // Strip the Origin & Referer headers so Spring Security CORS never fires.
    // The proxy forwards requests server-side — CORS is a browser concern only.
    onProxyReq: function (proxyReq) {
      proxyReq.removeHeader('Origin');
      proxyReq.removeHeader('Referer');
    }
  }
};
