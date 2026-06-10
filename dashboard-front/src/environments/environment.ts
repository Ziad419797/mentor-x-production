// Development environment — API calls go through the Angular proxy (proxy.conf.js)
// which forwards /api → http://localhost:8081
// No CORS issues because the proxy strips Origin/Referer headers before forwarding.
export const environment = {
  production: false,
  apiBase: ''  // relative path — proxy handles forwarding to localhost:8081
};
