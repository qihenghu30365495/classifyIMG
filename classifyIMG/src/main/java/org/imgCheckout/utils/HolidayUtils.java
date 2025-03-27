package org.imgCheckout.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class HolidayUtils {
    private static final String API_URL = "https://api.jiejiariapi.com/v1/is_holiday";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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