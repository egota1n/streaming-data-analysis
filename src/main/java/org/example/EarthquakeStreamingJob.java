package org.example;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class EarthquakeStreamingJob {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("localhost:29092")
                .setTopics("earthquakes-processed")
                .setGroupId("flink-earthquake-consumer")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> stream = env.fromSource(
                source,
                WatermarkStrategy.noWatermarks(),
                "Kafka Source"
        );

        DataStream<String> strongQuakes = stream
                .filter(json -> {
                    try {
                        EarthquakeEvent event = objectMapper.readValue(json, EarthquakeEvent.class);
                        return event.getMagnitude() > 4.0;
                    } catch (Exception e) {
                        System.err.println("Error parsing JSON: " + e.getMessage());
                        return false;
                    }
                })
                .name("Strong Earthquakes Filter");

        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("localhost:29092")
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic("earthquakes-strong")
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .build()
                )
                .build();

        strongQuakes.sinkTo(sink)
                .name("Kafka Sink");

        System.out.println("Starting Flink job...");
        env.execute("Earthquake Monitoring Job");
    }
}