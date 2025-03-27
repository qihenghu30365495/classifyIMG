package org.imgCheckout.utils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathValidator {
    public static boolean validateInputPath(String path, JFrame parent) {
        if (path == null || path.trim().isEmpty()) {
            showError(parent, "路径不能为空", "路径错误");
            return false;
        }

        Path pathObj = Paths.get(path);
        if (!Files.exists(pathObj)) {
            showError(parent, "路径不存在：" + path, "路径错误");
            return false;
        }

        if (!Files.isDirectory(pathObj)) {
            showError(parent, "必须选择目录：" + path, "路径类型错误");
            return false;
        }

        return true;
    }

    public static boolean validateOutputPath(String inputPath, String outputPath, JFrame parent) {
        if (outputPath == null || outputPath.trim().isEmpty()) {
            outputPath = inputPath;
        }

        Path outputPathObj = Paths.get(outputPath);
        if (!Files.exists(outputPathObj)) {
            int choice = JOptionPane.showConfirmDialog(parent, 
                "输出目录不存在，是否创建？", 
                "目录确认", 
                JOptionPane.YES_NO_OPTION);
            
            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }
            try {
                Files.createDirectories(outputPathObj);
            } catch (Exception e) {
                showError(parent, "目录创建失败：" + e.getMessage(), "创建错误");
                return false;
            }
        }

        if (!Files.isWritable(outputPathObj)) {
            showError(parent, "没有写入权限：" + outputPath, "权限错误");
            return false;
        }

        return true;
    }

    private static void showError(JFrame parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, 
            message,
            title,
            JOptionPane.ERROR_MESSAGE);
    }
}