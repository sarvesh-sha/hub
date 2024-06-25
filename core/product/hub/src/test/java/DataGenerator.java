import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DataGenerator {
    public static void main(String[] args) {
        // Example epoch seconds
        long epochSeconds = 1718374034L;

        // Convert epoch seconds to Instant
        Instant instant = Instant.ofEpochSecond(epochSeconds);

        // Define the time zone
        ZoneId zoneId = ZoneId.of("America/New_York"); // You can specify any time zone

        // Convert Instant to ZonedDateTime
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);

        // Print the ZonedDateTime
        System.out.println("ZonedDateTime: " + zonedDateTime);
    }
}