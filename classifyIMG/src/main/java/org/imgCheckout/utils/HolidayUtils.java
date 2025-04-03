package org.imgCheckout.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HolidayUtils {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/holiday?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    public static Map<LocalDate, Boolean> batchCheckHolidays(Set<LocalDate> dates) {
        Map<LocalDate, Boolean> results = new HashMap<>();
        
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD)) {
            String sql = "SELECT date, is_holiday FROM holidays WHERE date IN (" + 
                String.join(",", Collections.nCopies(dates.size(), "?")) + ")";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                int index = 1;
                for (LocalDate date : dates) {
                    pstmt.setString(index++, date.format(DATE_FORMATTER));
                }
                
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    LocalDate resultDate = LocalDate.parse(rs.getString("date"), DATE_FORMATTER);
                    boolean isHoliday = "true".equalsIgnoreCase(rs.getString("is_holiday"));
                    results.put(resultDate, isHoliday);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // 补充未查询到的日期默认值
        for (LocalDate date : dates) {
            results.putIfAbsent(date, false);
        }
        return results;
    }
}