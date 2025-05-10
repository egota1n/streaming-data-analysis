package org.example;

import java.net.URI;
import java.net.URISyntaxException;

public class EarthquakeMonitoringApp {
    public static void main(String[] args) throws URISyntaxException {
        // Инициализация Kafka продюсера
        KafkaProducerService producerService = new KafkaProducerService();

        // Подключение к WebSocket
        String wsUrl = "wss://www.seismicportal.eu/standing_order/websocket";
        EarthquakeWebSocketClient client = new EarthquakeWebSocketClient(new URI(wsUrl), producerService);
        client.connect();

        // Запуск анализатора
        EarthquakeAnalyzer analyzer = new EarthquakeAnalyzer();
        new Thread(analyzer::analyze).start();

        // Shutdown hook для корректного завершения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
            producerService.close();
            System.out.println("Application shutdown completed");
        }));
    }
}