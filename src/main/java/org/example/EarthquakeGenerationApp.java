package org.example;

public class EarthquakeGenerationApp {
    public static void main(String[] args) {
        try {
            KafkaProducerService producerService = new KafkaProducerService();

            EarthquakeDataGenerator dataGenerator = new EarthquakeDataGenerator();

            for (int i = 0; i < 100; i++) {
                String earthquakeData = dataGenerator.generateEarthquakeData();
                producerService.processEarthquakeData(earthquakeData);

                Thread.sleep(1);
            }

            producerService.close();
            System.out.println("All data sent to Kafka");
        } catch (InterruptedException e) {
            System.err.println("Error in data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}