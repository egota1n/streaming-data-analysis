package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.Random;

public class EarthquakeDataGenerator {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    private static final Random random = new Random();

    public String generateEarthquakeData() {
        EarthquakeEvent event = new EarthquakeEvent(
                Instant.now(),
                -90.0 + (180.0 * random.nextDouble()),
                -180.0 + (360.0 * random.nextDouble()),
                2.0 + (random.nextDouble() * 8.0),
                "Generated Region"
        );

        try {
            return objectMapper.writeValueAsString(createRawData(event));
        } catch (JsonProcessingException e) {
            System.err.println("Error generating earthquake data: " + e.getMessage());
            return null;
        }
    }

    private RawEarthquakeData createRawData(EarthquakeEvent event) {
        RawEarthquakeData rawData = new RawEarthquakeData();

        rawData.setAction(random.nextBoolean() ? "create" : "update");

        rawData.setData(new EarthquakeData(event));
        return rawData;
    }

    public static class RawEarthquakeData {
        private String action;
        private EarthquakeData data;

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public EarthquakeData getData() {
            return data;
        }

        public void setData(EarthquakeData data) {
            this.data = data;
        }
    }

    public static class EarthquakeData {
        private String type = "Feature";
        private EarthquakeGeometry geometry;
        private String id;
        private EarthquakeProperties properties;

        public EarthquakeData(EarthquakeEvent event) {
            this.geometry = new EarthquakeGeometry(event);
            this.id = generateUniqueId();
            this.properties = new EarthquakeProperties(event);
        }

        public String getType() {
            return type;
        }

        public EarthquakeGeometry getGeometry() {
            return geometry;
        }

        public String getId() {
            return id;
        }

        public EarthquakeProperties getProperties() {
            return properties;
        }

        private String generateUniqueId() {
            return "20250511_" + random.nextInt(10000); // Пример генерации уникального ID
        }
    }

    public static class EarthquakeGeometry {
        private String type = "Point";
        private double[] coordinates;

        public EarthquakeGeometry(EarthquakeEvent event) {
            this.coordinates = new double[] { event.getLon(), event.getLat(), event.getDepth() };
        }

        public String getType() {
            return type;
        }

        public double[] getCoordinates() {
            return coordinates;
        }
    }

    public static class EarthquakeProperties {
        private String source_id = "1807036";
        private String source_catalog = "EMSC-RTS";
        private String lastupdate = Instant.now().toString();
        private String time;
        private String flynn_region = "Generated Region";
        private double lat;
        private double lon;
        private double depth;
        private String evtype = "ke";
        private String auth = "CSN";
        private double mag;
        private String magtype = "ml";
        private String unid;

        public EarthquakeProperties(EarthquakeEvent event) {
            this.time = event.getTime().toString();
            this.lat = event.getLat();
            this.lon = event.getLon();
            this.depth = event.getDepth();
            this.mag = event.getMagnitude();
            this.unid = "20250511_" + random.nextInt(10000); // Пример генерации уникального ID
        }

        public String getSource_id() {
            return source_id;
        }

        public String getSource_catalog() {
            return source_catalog;
        }

        public String getLastupdate() {
            return lastupdate;
        }

        public String getTime() {
            return time;
        }

        public String getFlynn_region() {
            return flynn_region;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public double getDepth() {
            return depth;
        }

        public String getEvtype() {
            return evtype;
        }

        public String getAuth() {
            return auth;
        }

        public double getMag() {
            return mag;
        }

        public String getMagtype() {
            return magtype;
        }

        public String getUnid() {
            return unid;
        }
    }

    public static class EarthquakeEvent {
        private Instant time;
        private double lat;
        private double lon;
        private double magnitude;
        private double depth;
        private String region;

        public EarthquakeEvent(Instant time, double lat, double lon, double magnitude, String region) {
            this.time = time;
            this.lat = lat;
            this.lon = lon;
            this.magnitude = magnitude;
            this.depth = depth;
            this.region = region;
        }

        public Instant getTime() {
            return time;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public double getMagnitude() {
            return magnitude;
        }

        public double getDepth() {
            return depth;
        }

        public String getRegion() {
            return region;
        }
    }
}