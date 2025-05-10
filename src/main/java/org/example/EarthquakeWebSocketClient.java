package org.example;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class EarthquakeWebSocketClient extends WebSocketClient {

    private final KafkaProducerService producerService;

    public EarthquakeWebSocketClient(URI serverUri, KafkaProducerService producerService) {
        super(serverUri);
        this.producerService = producerService;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to WebSocket server");
    }

    @Override
    public void onMessage(String message) {
        // Обработка входящих данных о землетрясениях
        producerService.processEarthquakeData(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from WebSocket server: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}