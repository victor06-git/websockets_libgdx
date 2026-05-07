package com.vasensio.websockets.lwjgl3;

import com.vasensio.websockets.WsBridge;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DesktopWsPositionSender implements WsBridge.PositionSender {
    private final WebSocketClient client;
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final String id;

    public DesktopWsPositionSender(String url, String id) {
        this.id = id == null ? "desktop" : id;
        URI uri = URI.create(url);
        this.client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                open.set(true);
                System.out.println("[WS] Connected to " + url);
            }

            @Override
            public void onMessage(String message) {
                // Server doesn't need to send anything for this task; keep for debugging.
                System.out.println("[WS] Server: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                open.set(false);
                System.out.println("[WS] Closed: " + code + " " + reason);
            }

            @Override
            public void onError(Exception ex) {
                open.set(false);
                System.err.println("[WS] Error: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        // Non-blocking connect; messages will start sending once onOpen fires.
        this.client.connect();
    }

    @Override
    public void send(float x, float y) {
        if (!isOpen()) return;

        // Minimal JSON expected by server/index.js
        // {"type":"pos","id":"desktop","x":123.0,"y":45.0}
        String payload = String.format(Locale.US,
                "{\"type\":\"pos\",\"id\":\"%s\",\"x\":%.2f,\"y\":%.2f}",
                id, x, y);
        client.send(payload);
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception ignored) {
        }
        open.set(false);
    }

    @Override
    public boolean isOpen() {
        return open.get() && client.isOpen();
    }
}
