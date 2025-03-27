package org.imgCheckout.utils;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

    private static final Pattern FILE_PATTERN = Pattern.compile("^(IMG|VID)_(\\d{8})_.+");
    private static final Set<String> IMAGE_EXT = new HashSet<>(Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"));
    private static final Set<String> VIDEO_EXT = new HashSet<>(Set.of(
            "mp4", "mov", "avi", "mkv", "flv", "wmv"));

    public static String extractDateFromFileName(String fileName) {
        Matcher matcher = FILE_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    public static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static String determineCategoryKey(String ext) {
        if (IMAGE_EXT.contains(ext)) return "nonStandardImage";
        if (VIDEO_EXT.contains(ext)) return "nonStandardVideo";
        return "other";
    }

    public static String getCategoryDir(String categoryKey) {
        switch (categoryKey) {
            case "nonStandardImage": return "非标准图片";
            case "nonStandardVideo": return "非标准视频";
            case "other": return "其它文件";
            default: throw new IllegalArgumentException("未知分类: " + categoryKey);
        }
    }

    public static Path resolveDuplicate(Path path) {
        if (!java.nio.file.Files.exists(path)) return path;

        String baseName = getBaseName(path.getFileName().toString());
        String ext = getFileExtension(path.getFileName().toString());
        int counter = 1;

        while (true) {
            String newName = String.format("%s_%d%s", baseName, counter, ext.isEmpty() ? "" : "." + ext);
            Path newPath = path.resolveSibling(newName);
            if (!java.nio.file.Files.exists(newPath)) return newPath;
            counter++;
        }
    }

    public static String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}