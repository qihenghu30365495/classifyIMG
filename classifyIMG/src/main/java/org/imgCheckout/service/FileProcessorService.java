package org.imgCheckout.service;

import org.imgCheckout.gui.ImageOrganizerGUI;
import org.imgCheckout.utils.DateUtils;
import org.imgCheckout.utils.FileUtils;
import org.imgCheckout.utils.HolidayUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

public class FileProcessorService {

    private static final ExecutorService holidayQueryExecutor = Executors.newFixedThreadPool(10);
    private static final Map<String, Integer> counters = new HashMap<>();
    private int totalFiles = 0;
    private int processedFiles = 0;
    private ImageOrganizerGUI gui;
    private DirectoryService directoryService;

    public FileProcessorService(ImageOrganizerGUI gui) {
        this.gui = gui;
        this.directoryService = new DirectoryService();
    }

    public void resetCounters() {
        totalFiles = 0;
        processedFiles = 0;
    }

    public void processFiles(String path, String outputPath, boolean classifyByHoliday, boolean processSubfolders, boolean overwriteDuplicates) throws IOException {
        counters.put("total", 0);
        counters.put("standard", 0);
        counters.put("nonStandardImage", 0);
        counters.put("nonStandardVideo", 0);
        counters.put("other", 0);

        Path inputPath = Paths.get(path);
        Path rootPath = outputPath.isEmpty() 
            ? inputPath.resolve("output") 
            : Paths.get(outputPath);

        // 确保输出目录存在
        directoryService.createDirectory(rootPath, gui);
        directoryService.createCategoryDirs(rootPath, gui);

        // 扫描输入路径的文件数量
        if (processSubfolders) {
            totalFiles = countFilesInDirectoryRecursively(inputPath);
        } else {
            totalFiles = countFilesInDirectory(inputPath);
        }

        // 处理文件时使用正确的输入路径和输出路径
        if (processSubfolders) {
            processFilesRecursively(inputPath, classifyByHoliday, overwriteDuplicates, rootPath);
        } else {
            try (DirectoryStream<Path> processStream = Files.newDirectoryStream(inputPath)) {
                for (Path filePath : processStream) {
                    if (Files.isRegularFile(filePath)) {
                        processFile(filePath, rootPath, classifyByHoliday, overwriteDuplicates);
                        processedFiles++;
                        updateProgress();
                    }
                }
            }
        }

        String result = "\n处理完成！\n" +
                "总文件数: " + counters.get("total") + "\n" +
                "标准文件: " + counters.get("standard") + "\n" +
                "非标准图片: " + counters.get("nonStandardImage") + "\n" +
                "非标准视频: " + counters.get("nonStandardVideo") + "\n" +
                "其他文件: " + counters.get("other");
        gui.taskDone(result);
    }

    private int countFilesInDirectory(Path directory) throws IOException {
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countFilesInDirectoryRecursively(Path directory) throws IOException {
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    count++;
                } else if (Files.isDirectory(path)) {
                    count += countFilesInDirectoryRecursively(path);
                }
            }
        }
        return count;
    }

    private void processFilesRecursively(Path directory, boolean classifyByHoliday, boolean overwriteDuplicates, Path rootPath) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    processFile(path, rootPath, classifyByHoliday, overwriteDuplicates);
                    processedFiles++;
                    updateProgress();
                } else if (Files.isDirectory(path)) {
                    processFilesRecursively(path, classifyByHoliday, overwriteDuplicates, rootPath);
                }
            }
        }
    }

    private void updateProgress() {
        int progress = (int) ((processedFiles / (double) totalFiles) * 100);
        gui.updateProgress(progress);
    }

    private void processFile(Path filePath, Path rootPath, boolean classifyByHoliday, boolean overwriteDuplicates) {
        counters.put("total", counters.get("total") + 1);
        String fileName = filePath.getFileName().toString();
        String dateStr = FileUtils.extractDateFromFileName(fileName);

        try {
            if (dateStr != null) {
                handleStandardFile(filePath, rootPath, dateStr, classifyByHoliday, overwriteDuplicates);
                counters.put("standard", counters.get("standard") + 1);
                gui.updateLog("处理标准文件: " + fileName);
            } else {
                handleNonStandardFile(filePath, rootPath, fileName, overwriteDuplicates);
                gui.updateLog("处理非标准文件: " + fileName);
            }
        } catch (Exception e) {
            handleError(fileName, e);
        }
    }

    private void handleStandardFile(Path file, Path root, String dateStr, boolean classifyByHoliday, boolean overwriteDuplicates) throws IOException {
        LocalDate date = DateUtils.parseDate(dateStr);
        String monthDir = DateUtils.formatDateToMonth(date);
        Path monthPath = root.resolve(monthDir);
        if (directoryService.createDirectory(monthPath, gui)) {
            gui.updateLog("创建文件夹: " + monthPath);
        }

        if (classifyByHoliday) {
            Set<LocalDate> datesToCheck = new HashSet<>();
            datesToCheck.add(date);
        Map<LocalDate, Boolean> holidayMap = HolidayUtils.batchCheckHolidays(datesToCheck);
        String dayType = holidayMap.get(date) ? "节假日" : "工作日";
            Path dayTypePath = monthPath.resolve(dayType);
            if (directoryService.createDirectory(dayTypePath, gui)) {
                gui.updateLog("创建文件夹: " + dayTypePath);
            }
            moveFile(file, dayTypePath, overwriteDuplicates);
        } else {
            moveFile(file, monthPath, overwriteDuplicates);
        }
    }

    private void handleNonStandardFile(Path file, Path root, String fileName, boolean overwriteDuplicates) {
        String ext = FileUtils.getFileExtension(fileName).toLowerCase();
        String categoryKey = FileUtils.determineCategoryKey(ext);
        String categoryDir = FileUtils.getCategoryDir(categoryKey);
        Path targetDir = root.resolve(categoryDir);
        if (directoryService.createDirectory(targetDir, gui)) {
            gui.updateLog("创建文件夹: " + targetDir);
        }

        try {
            moveFile(file, targetDir, overwriteDuplicates);
            counters.put(categoryKey, counters.get(categoryKey) + 1);
        } catch (IOException e) {
            throw new RuntimeException("移动文件失败: " + fileName, e);
        }
    }

    private void moveFile(Path source, Path targetDir, boolean overwriteDuplicates) throws IOException {
        Path destination = targetDir.resolve(source.getFileName());
        if (overwriteDuplicates) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            gui.updateLog("移动文件: " + source.getFileName() + " => " + destination + "（覆盖）");
        } else {
            if (!Files.exists(destination)) {
                Files.move(source, destination);
                gui.updateLog("移动文件: " + source.getFileName() + " => " + destination);
            } else {
                gui.updateLog("跳过重复文件: " + source.getFileName());
            }
        }
    }

    private void handleError(String fileName, Exception e) {
        String errorType = e instanceof java.time.DateTimeException ? "日期错误" :
                e instanceof IOException ? "IO错误" : "其他错误";
        gui.updateLog("处理失败 [" + errorType + "] " + fileName + ": " + e.getMessage());
    }
}