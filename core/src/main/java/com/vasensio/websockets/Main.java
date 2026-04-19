package com.vasensio.websockets;

import java.net.URI;
import java.net.URISyntaxException;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    private float x = 140f;
    private float y = 210f;

    private PositionWebSocketClient wsClient;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        startWebSocket();
    }

    private void startWebSocket() {
        String host = "ws://localhost:8080";
        if (Gdx.app.getType() == ApplicationType.Android) {
            // Android emulator uses 10.0.2.2 to reach host localhost
            host = "ws://10.0.2.2:8080";
        }
        try {
            wsClient = new PositionWebSocketClient(new URI(host));
            new Thread(() -> wsClient.connect()).start();
        } catch (URISyntaxException e) {
            Gdx.app.log("WS", "Invalid URI: " + e.getMessage());
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        boolean moved = false;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            x -= 200f * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            x += 200f * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            y += 200f * delta;
            moved = true;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            y -= 200f * delta;
            moved = true;
        }

        if (moved) sendPosition();

        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, x, y);
        batch.end();
    }

    private void sendPosition() {
        if (wsClient != null && wsClient.isOpen()) {
            String msg = String.format("{\"x\":%.2f,\"y\":%.2f}", x, y);
            wsClient.send(msg);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static class PositionWebSocketClient extends WebSocketClient {
        PositionWebSocketClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            Gdx.app.log("WS", "Connected to server");
        }

        @Override
        public void onMessage(String message) {
            Gdx.app.log("WS", "Received: " + message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            Gdx.app.log("WS", "Closed: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            Gdx.app.log("WS", "Error: " + ex.getMessage());
        }
    }
}
