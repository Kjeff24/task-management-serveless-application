package org.example.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter {

    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    public static LocalDateTime convert(LocalDateTime localDateTime) {
        return LocalDateTime.parse(localDateTime.format(FORMATTER), FORMATTER);
    }
}
