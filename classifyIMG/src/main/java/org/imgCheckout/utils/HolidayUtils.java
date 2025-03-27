package org.imgCheckout.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;
import java.util.concurrent.*;
import java.util.*;

public class HolidayUtils {
    private static final String API_URL = "https://api.jiejiariapi.com/v1/is_holiday";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ExecutorService executor = Executors.newFixedThreadPool(20);
    
    public static Map<LocalDate, Boolean> batchCheckHolidays(Set<LocalDate> dates) {
        Map<LocalDate, Boolean> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (LocalDate date : dates) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    boolean isHoliday = isHoliday(date);
                    results.put(date, isHoliday);
                    TimeUnit.MILLISECONDS.sleep(50); // 控制请求频率
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, executor));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return results;
    }

    public static boolean isHoliday(LocalDate date) {
        String dateStr = date.format(DATE_FORMATTER);
        String apiUrlWithDate = API_URL + "?date=" + dateStr;
        try {
            URL url = new URL(apiUrlWithDate);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getBoolean("is_holiday");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}