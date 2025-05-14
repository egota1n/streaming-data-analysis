package org.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.time.Instant;

public class EarthquakeStreamingJob {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("kafka:9092")
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

        // Используем сериализуемый парсер
        DataStream<EarthquakeEvent> parsedStream = stream
                .map(new JsonToEventParser())
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<EarthquakeEvent>forBoundedOutOfOrderness(Duration.ofMinutes(1))
                                .withTimestampAssigner((event, timestamp) -> event.getTimestamp().toEpochMilli())
                )
                .name("Parse JSON and assign timestamps");

        // Используем сериализуемый сериализатор
        DataStream<String> strongQuakes = parsedStream
                .filter(event -> event.getMagnitude() > 4.0)
                .map(new EventToJsonSerializer())
                .name("Strong Earthquakes Filter");

        strongQuakes.sinkTo(
                KafkaSink.<String>builder()
                        .setBootstrapServers("kafka:9092")
                        .setRecordSerializer(
                                KafkaRecordSerializationSchema.builder()
                                        .setTopic("earthquakes-strong")
                                        .setValueSerializationSchema(new SimpleStringSchema())
                                        .build()
                        )
                        .build()
        ).name("Strong Quakes Kafka Sink");

        // Скользящее окно 60 минут с шагом 5 минут
        DataStream<String> slidingWindowResult = parsedStream
                .windowAll(SlidingEventTimeWindows.of(Time.minutes(5), Time.minutes(1)))
                .apply(new CountWindowFunction());

        slidingWindowResult.sinkTo(
                KafkaSink.<String>builder()
                        .setBootstrapServers("kafka:9092")
                        .setRecordSerializer(
                                KafkaRecordSerializationSchema.builder()
                                        .setTopic("earthquakes-stats-hourly")
                                        .setValueSerializationSchema(new SimpleStringSchema())
                                        .build()
                        )
                        .build()
        ).name("Windowed Kafka Sink");

        System.out.println("Starting Flink job...");
        env.execute("Earthquake Monitoring Job");
    }

    // Сериализуемый парсер JSON
    public static class JsonToEventParser implements org.apache.flink.api.common.functions.MapFunction<String, EarthquakeEvent> {
        private transient com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Override
        public EarthquakeEvent map(String value) throws Exception {
            if (objectMapper == null) {
                objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            }
            return objectMapper.readValue(value, EarthquakeEvent.class);
        }
    }

    // Сериализуемый сериализатор в JSON
    public static class EventToJsonSerializer implements org.apache.flink.api.common.functions.MapFunction<EarthquakeEvent, String> {
        private transient com.fasterxml.jackson.databind.ObjectMapper objectMapper;

        @Override
        public String map(EarthquakeEvent value) throws Exception {
            if (objectMapper == null) {
                objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
                        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            }
            return objectMapper.writeValueAsString(value);
        }
    }

    // Сериализуемая функция для оконной обработки
    public static class CountWindowFunction implements AllWindowFunction<EarthquakeEvent, String, TimeWindow> {
        @Override
        public void apply(TimeWindow window, Iterable<EarthquakeEvent> values, Collector<String> out) {
            long count = 0;
            for (EarthquakeEvent e : values) {
                count++;
            }
            out.collect(String.format(
                    "{\"window_start\": %d, \"window_end\": %d, \"count\": %d}",
                    window.getStart(),
                    window.getEnd(),
                    count
            ));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EarthquakeEvent {
        private Instant timestamp;
        private double latitude;
        private double longitude;
        private double magnitude;
        private String location;

        @JsonProperty("timestamp")
        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getMagnitude() {
            return magnitude;
        }

        public void setMagnitude(double magnitude) {
            this.magnitude = magnitude;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}