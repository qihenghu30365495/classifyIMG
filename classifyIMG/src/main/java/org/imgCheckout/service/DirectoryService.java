package org.imgCheckout.service;

import org.imgCheckout.gui.ImageOrganizerGUI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryService {

    public void createCategoryDirs(Path root, ImageOrganizerGUI gui) {
        if (createDirectory(root.resolve("非标准图片"), gui)) {
            gui.updateLog("创建文件夹: " + root.resolve("非标准图片"));
        }
        if (createDirectory(root.resolve("非标准视频"), gui)) {
            gui.updateLog("创建文件夹: " + root.resolve("非标准视频"));
        }
        if (createDirectory(root.resolve("其它文件"), gui)) {
            gui.updateLog("创建文件夹: " + root.resolve("其它文件"));
        }
    }

    public boolean createDirectory(Path path, ImageOrganizerGUI gui) {
        if (Files.exists(path)) {
            return false;
        }
        try {
            Files.createDirectories(path);
            return true;
        } catch (IOException e) {
            gui.updateLog("创建目录失败: " + path + " - " + e.getMessage());
            return false;
        }
    }
}