package org.example;

import java.net.URI;
import java.net.URISyntaxException;

public class EarthquakeMonitoringApp {
    public static void main(String[] args) throws URISyntaxException {
        KafkaProducerService producerService = new KafkaProducerService();

        String wsUrl = "wss://www.seismicportal.eu/standing_order/websocket";
        EarthquakeWebSocketClient client = new EarthquakeWebSocketClient(new URI(wsUrl), producerService);
        client.connect();

        EarthquakeAnalyzer analyzer = new EarthquakeAnalyzer();
        new Thread(analyzer::analyze).start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            client.close();
            producerService.close();
            System.out.println("Application shutdown completed");
        }));
    }
}