package org.example.shopservice;

import java.time.ZoneId;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopServiceApplication {

    public static void main(String[] args) {
        // Avoid invalid legacy timezone names (e.g., Europe/Kiev) when talking to Postgres
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Kyiv")));
        SpringApplication.run(ShopServiceApplication.class, args);
    }
}
