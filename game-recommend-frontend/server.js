const http = require('http');
const fs = require('fs');
const path = require('path');
const httpProxy = require('http-proxy');

const PORT = 8081;
const API_TARGET = 'http://localhost:8080';
const STATIC_DIR = path.join(__dirname, 'dist');
const GAME_IMAGES_DIR = path.join(__dirname, '..', 'generated-images', 'games');

const proxy = httpProxy.createProxyServer({ target: API_TARGET, changeOrigin: true });
proxy.on('error', (err, req, res) => {
  console.error('Proxy error:', err.message);
  res.writeHead(502, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ code: 502, message: 'Backend unavailable' }));
});

const mime = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2',
  '.woff': 'font/woff',
  '.map': 'application/json',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg'
};

const server = http.createServer((req, res) => {
  // 代理 API 请求到后端
  if (req.url.startsWith('/api')) {
    return proxy.web(req, res);
  }

  // 游戏图片从 generated-images/games/ 目录提供
  if (req.url.startsWith('/images/games/')) {
    const filename = path.basename(req.url);
    const filePath = path.join(GAME_IMAGES_DIR, filename);
    fs.readFile(filePath, (err, data) => {
      if (err) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Image not found');
        return;
      }
      const ext = path.extname(filePath);
      res.writeHead(200, { 'Content-Type': mime[ext] || 'application/octet-stream' });
      res.end(data);
    });
    return;
  }

  // 静态文件从 dist 目录提供
  let urlPath = req.url === '/' ? '/index.html' : req.url.split('?')[0];
  let filePath = path.join(STATIC_DIR, urlPath);

  fs.readFile(filePath, (err, data) => {
    if (err) {
      fs.readFile(path.join(STATIC_DIR, 'index.html'), (e, d) => {
        res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
        res.end(d);
      });
      return;
    }
    const ext = path.extname(filePath);
    res.writeHead(200, {
      'Content-Type': mime[ext] || 'application/octet-stream',
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Pragma': 'no-cache',
      'Expires': '0'
    });
    res.end(data);
  });
});

server.listen(PORT, () => {
  console.log(`Frontend: http://localhost:${PORT}`);
  console.log(`API proxy: /api → ${API_TARGET}`);
  console.log(`Game images: /images/games/ → ${GAME_IMAGES_DIR}`);
});
