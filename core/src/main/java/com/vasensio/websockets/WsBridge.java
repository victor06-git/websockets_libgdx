package com.vasensio.websockets;

public final class WsBridge {
    private WsBridge() {}

    public interface PositionSender {
        void send(float x, float y);
        void close();
        boolean isOpen();
    }

    private static PositionSender sender = null;

    public static void setSender(PositionSender s) {
        sender = s;
    }

    public static void sendPosition(float x, float y) {
        if (sender != null && sender.isOpen()) sender.send(x, y);
    }

    public static void close() {
        if (sender != null) sender.close();
        sender = null;
    }
}
