const { createServer } = require('http');
const { WebSocketServer } = require('ws');

const server = createServer();
const wss = new WebSocketServer({ server });

function safeJsonParse(data) {
  try {
    const text = typeof data === 'string' ? data : data.toString();
    return JSON.parse(text);
  } catch {
    return null;
  }
}

wss.on('connection', function connection(ws, req) {
  console.log('Nova connexió.', req?.socket?.remoteAddress ?? '');

  ws.on('error', console.error);

  ws.on('message', function message(data) {
    const msg = safeJsonParse(data);

    // Expected from the game:
    // { "type": "pos", "id": "player1", "x": 123.4, "y": 56.7 }
    if (msg && (msg.type === 'pos' || msg.type === 'position') && Number.isFinite(msg.x) && Number.isFinite(msg.y)) {
      const id = typeof msg.id === 'string' ? msg.id : 'player';
      console.log(`[${id}] position -> x=${msg.x} y=${msg.y}`);
      return;
    }

    // Fallback: allow raw "x,y" strings for quick manual tests.
    const text = typeof data === 'string' ? data : data.toString();
    const m = text.trim().match(/^(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)$/);
    if (m) {
      console.log(`[player] position -> x=${Number(m[1])} y=${Number(m[2])}`);
      return;
    }

    console.log('received (ignored): %s', text.trim());
  });

  ws.on('close', function close() {
    console.log('Tancant connexió.');
  });
});

const PORT = Number(process.env.PORT || 8888);
server.listen(PORT, function () {
  console.log(`listening on ws://localhost:${PORT}`);
});
