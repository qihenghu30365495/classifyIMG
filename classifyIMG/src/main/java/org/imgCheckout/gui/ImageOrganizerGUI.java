package org.imgCheckout.gui;

import org.imgCheckout.service.FileProcessorService;
import org.imgCheckout.utils.PathValidator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageOrganizerGUI extends JFrame {

    private JTextField pathField;
    private JTextField pathTextField;
    private JTextArea logArea;
    private JButton executeBtn;
    private JButton outputBrowseBtn;
    private JProgressBar progressBar;
    private JCheckBox classifyByHolidayCheckbox;
    private JMenuBar menuBar;
    private JMenu settingsMenu;
    private JCheckBoxMenuItem processSubfoldersCheckbox;
    private JCheckBoxMenuItem overwriteDuplicatesCheckbox;
    private FileProcessorService fileProcessorService;
    private JLabel timerLabel;
    private javax.swing.Timer timer;
    private long startTime;

    public ImageOrganizerGUI() {
        fileProcessorService = new FileProcessorService(this);
        initUI();
    }

    private void initUI() {
        setTitle("图片视频整理工具（^.^）        PowerBy - FIRELOVER");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 创建菜单栏
        menuBar = new JMenuBar();
        settingsMenu = new JMenu("设置");
        processSubfoldersCheckbox = new JCheckBoxMenuItem("是否处理子文件夹");
        overwriteDuplicatesCheckbox = new JCheckBoxMenuItem("是否覆盖重复文件");
        overwriteDuplicatesCheckbox.setSelected(true); // 默认勾选
        settingsMenu.add(processSubfoldersCheckbox);
        settingsMenu.add(overwriteDuplicatesCheckbox);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 路径输入区
        JPanel pathPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // 源路径面板
        JPanel sourcePathPanel = new JPanel(new BorderLayout(5, 5));
        pathField = new JTextField();
        JButton browseBtn = new JButton("浏览...");
        browseBtn.addActionListener(this::browseFolder);
        sourcePathPanel.add(new JLabel("目标文件夹："), BorderLayout.WEST);
        sourcePathPanel.add(pathField, BorderLayout.CENTER);
        sourcePathPanel.add(browseBtn, BorderLayout.EAST);
        
        // 输出路径面板
        JPanel outputPathPanel = new JPanel(new BorderLayout(5, 5));
        pathTextField = new JTextField();
        outputBrowseBtn = new JButton("浏览...");
        outputBrowseBtn.addActionListener(e -> chooseOutputPath());
        outputPathPanel.add(new JLabel("输出文件夹："), BorderLayout.WEST);
        outputPathPanel.add(pathTextField, BorderLayout.CENTER);
        outputPathPanel.add(outputBrowseBtn, BorderLayout.EAST);
        
        pathPanel.add(sourcePathPanel);
        pathPanel.add(outputPathPanel);

        // 日志区
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);

        // 勾选按钮
        classifyByHolidayCheckbox = new JCheckBox("是否根据节假日分类");
        classifyByHolidayCheckbox.addActionListener(e -> {
            if (classifyByHolidayCheckbox.isSelected()) {
                logArea.append("将调用外部api接口查询节假日，速度会大幅降低   ！！！\n");
            } else {
                String logText = logArea.getText();
                logText = logText.replace("将调用外部api接口查询节假日，速度会大幅降低   ！！！\n", "");
                logArea.setText(logText);
            }
        });

        // 按钮面板
        JPanel buttonPanel = new JPanel(new BorderLayout(5, 5));
        executeBtn = new JButton("开始整理");
        // 计时器显示区域
        timerLabel = new JLabel("处理时间：00:00:00");
        buttonPanel.add(timerLabel, BorderLayout.WEST);

        executeBtn.addActionListener(e -> {
            String path = pathField.getText().trim();
            String outputPath = pathTextField.getText().trim();
            
            if (!PathValidator.validateInputPath(path, this)) {
                return;
            }
            if (!PathValidator.validateOutputPath(path, outputPath, this)) {
                return;
            }

            executeBtn.setEnabled(false);
            fileProcessorService.resetCounters();
            progressBar.setValue(0);
            progressBar.setString("0%");
            logArea.setText("");
            // 重置计时器
            if (timer != null) {
                timer.stop();
                timerLabel.setText("处理时间：00:00:00");
            }

            boolean classifyByHoliday = classifyByHolidayCheckbox.isSelected();
            boolean processSubfolders = processSubfoldersCheckbox.isSelected();
            boolean overwriteDuplicates = overwriteDuplicatesCheckbox.isSelected();
            new Thread(() -> {
                try {
                    fileProcessorService.processFiles(path, outputPath, classifyByHoliday, processSubfolders, overwriteDuplicates);
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        logArea.append("处理文件时发生错误: " + ex.getMessage() + "\n");
                        executeBtn.setEnabled(true);
                    });
                }
            }).start();
            startTime = System.currentTimeMillis();
            timer = new javax.swing.Timer(100, event -> {
                long elapsedTime = System.currentTimeMillis() - startTime;
                SimpleDateFormat sdf = new SimpleDateFormat("00:mm:ss.SSS");
                timerLabel.setText("处理时间：" + sdf.format(new Date(elapsedTime)));
            });
            timer.start();
        });
        buttonPanel.add(progressBar, BorderLayout.CENTER);
        JPanel checkboxAndButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        checkboxAndButtonPanel.add(classifyByHolidayCheckbox);
        checkboxAndButtonPanel.add(executeBtn);
        buttonPanel.add(checkboxAndButtonPanel, BorderLayout.EAST);

        mainPanel.add(pathPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void browseFolder(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void updateLog(String log) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(log + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public void updateProgress(int progress) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(progress);
            progressBar.setString(progress + "%");
        });
    }

    public void taskDone(String result) {
        SwingUtilities.invokeLater(() -> {
            executeBtn.setEnabled(true);
            progressBar.setValue(100);
            logArea.append(result);
        });
        if (timer != null) {
            timer.stop();
        }
    }

    private void chooseOutputPath() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            pathTextField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}