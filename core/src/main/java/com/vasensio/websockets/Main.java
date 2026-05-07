package com.vasensio.websockets;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
// IMPORTS ESPECÍFICOS DE GDX-WEBSOCKETS
import com.github.czyzby.websocket.WebSocket;
import com.github.czyzby.websocket.WebSocketListener;
import com.github.czyzby.websocket.WebSockets;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture spriteSheet;
    private float stateTime;
    private Animation<TextureRegion> walkAnim, idleAnim, currentAnim;

    private final float SPEED = 200f;
    private boolean mirandoDerecha = true;

    private OrthographicCamera camera;
    private float vx = 0;

    // Parallax
    private Texture layer1, layer2, layer3;
    private float x1 = 0, x2 = 0, x3 = 0;

    // Joystick
    private Stage stage;
    private Touchpad touchpad;
    private Skin touchpadSkin;

    private float wsSendAccumulator = 0f;
    private float worldX = 0f;
    private float worldY = 40f;

    // VARIABLES WEBSOCKET
    private WebSocket socket;
    private String address = "localhost";
    private int port = 8888;

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // CONFIGURAR JOYSTICK
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        touchpadSkin = new Skin();
        touchpadSkin.add("bg", new Texture(Gdx.files.internal("Joystick.png")));
        touchpadSkin.add("knob", new Texture(Gdx.files.internal("SmallHandleFilled.png")));

        Touchpad.TouchpadStyle style = new Touchpad.TouchpadStyle();
        style.background = touchpadSkin.getDrawable("bg");
        style.knob = touchpadSkin.getDrawable("knob");

        touchpad = new Touchpad(10, style);
        touchpad.setBounds(50, 50, 150, 150);
        stage.addActor(touchpad);

        // TEXTURAS Y ANIMACIONES
        layer1 = new Texture(Gdx.files.internal("background_layer_1.png"));
        layer2 = new Texture(Gdx.files.internal("background_layer_2.png"));
        layer3 = new Texture(Gdx.files.internal("background_layer_3.png"));
        layer1.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        layer2.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        layer3.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        spriteSheet = new Texture(Gdx.files.internal("animated_beast.png"));
        TextureRegion[][] tmp = TextureRegion.split(spriteSheet, 32, 32);
        idleAnim = new Animation<>(0.15f, tmp[0]);
        walkAnim = new Animation<>(0.1f, tmp[1]);
        currentAnim = idleAnim;

        // --- INICIALIZAR WEBSOCKET ---
        initializeWebSocket();
    }

    private void initializeWebSocket() {
        if (Gdx.app.getType() == Application.ApplicationType.Android) {
            address = "10.0.2.2"; // IP para conectar al PC desde el emulador
        }

        // Creamos el socket usando la factoría de la librería
        socket = WebSockets.newSocket(WebSockets.toWebSocketUrl(address, port));
        socket.setSendGracefully(false);

        // Añadimos el listener (clase interna definida abajo)
        socket.addListener(new MyWSListener());

        Gdx.app.log("WS", "Intentando conectar a " + address);
        socket.connect();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        stateTime += delta;
        wsSendAccumulator += delta;

        vx = 0;
        float knobX = touchpad.getKnobPercentX();

        if (Math.abs(knobX) > 0.15f) {
            vx = knobX * SPEED;
            mirandoDerecha = (knobX > 0);
            currentAnim = walkAnim;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            vx = -SPEED; mirandoDerecha = false; currentAnim = walkAnim;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            vx = SPEED; mirandoDerecha = true; currentAnim = walkAnim;
        } else {
            currentAnim = idleAnim;
        }

        // Parallax
        x1 += vx * 0.1f * delta;
        x2 += vx * 0.4f * delta;
        x3 += vx * 0.9f * delta;
        worldX += vx * delta;

        // --- ENVÍO DE DATOS POR WEBSOCKET (Throttle 10Hz) ---
        if (wsSendAccumulator >= 0.10f) {
            wsSendAccumulator = 0f;
            if (socket != null && socket.isOpen()) {
                // Enviamos el mensaje en el formato que espera tu servidor Node
                socket.send("Nom: Hero, Posx: " + worldX);
            }
        }

        ScreenUtils.clear(0, 0, 0, 1);
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        drawBackground(layer1, x1);
        drawBackground(layer2, x2);
        drawBackground(layer3, x3);

        TextureRegion currentFrame = currentAnim.getKeyFrame(stateTime, true);
        float posX = (Gdx.graphics.getWidth() - 96) / 2f;
        if (mirandoDerecha) {
            batch.draw(currentFrame, posX, 40, 96, 96);
        } else {
            batch.draw(currentFrame, posX + 96, 40, -96, 96);
        }
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void drawBackground(Texture tex, float posX) {
        batch.draw(tex, 0, 20, Gdx.graphics.getWidth(), Gdx.graphics.getHeight() * 0.8f,
            (int)posX, 0, tex.getWidth(), tex.getHeight(), false, false);
    }

    @Override
    public void dispose() {
        batch.dispose();
        spriteSheet.dispose();
        if (socket != null) socket.close();
        stage.dispose();
    }

    // --- ESCUCHADOR DE EVENTOS WEBSOCKET ---
    class MyWSListener implements WebSocketListener {
        @Override
        public boolean onOpen(WebSocket webSocket) {  // ✅ Sin java.net.http.
            Gdx.app.log("WS", "✅ WebSocket conectado");
            return false;
        }

        @Override
        public boolean onClose(WebSocket webSocket, int closeCode, String reason) {
            Gdx.app.log("WS", "❌ WebSocket cerrado: " + reason);
            return false;
        }

        @Override
        public boolean onMessage(WebSocket webSocket, String packet) {
            Gdx.app.log("WS", "📨 Mensaje: " + packet);
            return false;
        }

        @Override
        public boolean onMessage(WebSocket webSocket, byte[] packet) {
            return false;
        }

        @Override
        public boolean onError(WebSocket webSocket, Throwable error) {
            Gdx.app.log("WS", "⚠️ Error: " + error.getMessage());
            return false;
        }
    }
}
