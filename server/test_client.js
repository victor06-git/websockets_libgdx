const WebSocket = require('ws');

const ws = new WebSocket('ws://localhost:8888');

ws.on('open', function open() {
  console.log('Connected to server');
  let x = 100;
  let y = 120;
  const iv = setInterval(() => {
    x += 5;
    y += 2;
    const msg = { type: 'pos', id: 'test_client', x, y };
    ws.send(JSON.stringify(msg));
  }, 250);

  setTimeout(() => {
    clearInterval(iv);
    ws.close();
  }, 5000);
});

ws.on('message', function message(data) {
  try {
    const parsed = JSON.parse(data);
    console.log('Server:', parsed);
  } catch (e) {
    console.log('Server (raw):', data.toString());
  }
});

ws.on('close', () => console.log('Disconnected'));
ws.on('error', (err) => console.error('Error:', err));
