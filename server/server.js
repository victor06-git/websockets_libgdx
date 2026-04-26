const http = require('http');
const fs = require('fs');
const path = require('path');
const WebSocket = require('ws');

const server = http.createServer((req, res) => {
  let reqPath = req.url === '/' ? '/index.html' : req.url;
  let filePath = path.join(__dirname, 'public', reqPath);
  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    const ext = path.extname(filePath);
    const map = {'.html':'text/html', '.js':'application/javascript', '.css':'text/css'};
    res.writeHead(200, {'Content-Type': map[ext] || 'text/plain'});
    res.end(data);
  });
});

const wss = new WebSocket.Server({ server });

wss.on('connection', function connection(ws) {
  console.log('Client connected');
  ws.on('message', function incoming(message) {
    console.log('received: %s', message);
    // broadcast to all clients
    wss.clients.forEach(function each(client) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(message);
      }
    });
  });
  ws.send(JSON.stringify({msg:'welcome'}));
});

const port = process.env.PORT ? Number(process.env.PORT) : 8080;
server.listen(port, () => {
  console.log('Server listening on http://localhost:' + port);
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error('Port ' + port + ' already in use. Use another PORT or stop the process using it.');
  } else {
    console.error('Server error:', err);
  }
  process.exit(1);
});
