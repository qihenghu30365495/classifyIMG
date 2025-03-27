package org.imgCheckout.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("uuuuMMdd")
            .withResolverStyle(ResolverStyle.STRICT);

    public static LocalDate parseDate(String dateStr) {
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    public static String formatDateToMonth(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}