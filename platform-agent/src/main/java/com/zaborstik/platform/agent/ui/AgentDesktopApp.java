package com.zaborstik.platform.agent.ui;

import java.awt.event.KeyEvent;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.AWTException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AgentDesktopApp {

    private static JTextArea resultArea;
    private static JTextField queryField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AgentDesktopApp::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Automation Platform – Запрос");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(600, 400));

        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        JLabel promptLabel = new JLabel("Введите запрос:");
        queryField = new JTextField();
        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(queryField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Отправить");

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.add(inputPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(sendButton);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(580, 260));

        frame.getContentPane().setLayout(new BorderLayout(8, 8));
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);
        frame.getContentPane().add(scrollPane, BorderLayout.CENTER);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String query = queryField.getText().trim();
                if (query.isEmpty()) {
                    resultArea.append("Пожалуйста, введите запрос.\n");
                    return;
                }

                resultArea.append("Отправлен запрос: " + query + "\n");

                try {
                    simulateWinD();
                    resultArea.append("Выполнено сворачивание окон.\n");

                    openBrowserWithQuery(query);
                    resultArea.append("Браузер открыт с поисковым запросом.\n");

                    queryField.setText("");

                } catch (AWTException | IOException ex) {
                    ex.printStackTrace();
                    resultArea.append("Ошибка: " + ex.getMessage() + "\n");
                }
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Проверяет, является ли текущая ОС macOS
     * @return true если macOS, false в противном случае
     */
    private static boolean isMacOS() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("osx");
    }

    /**
     * Сворачивает окна в зависимости от ОС
     * @throws AWTException если ошибка работы с Robot
     */
    private static void simulateWinD() throws AWTException {
        Robot robot = new Robot();

        if (isMacOS()) {
            // Cmd+H — свернуть текущее окно
            robot.keyPress(KeyEvent.VK_META);
            robot.keyPress(KeyEvent.VK_H);
            robot.delay(50);
            robot.keyRelease(KeyEvent.VK_H);
            robot.keyRelease(KeyEvent.VK_META);
            robot.delay(200);
        } else {
            // Windows/Linux: Win+D — показать рабочий стол
            robot.keyPress(KeyEvent.VK_WINDOWS);
            robot.keyPress(KeyEvent.VK_D);
            robot.delay(50);
            robot.keyRelease(KeyEvent.VK_D);
            robot.keyRelease(KeyEvent.VK_WINDOWS);
            robot.delay(200);
        }
    }

    /**
     * Открывает браузер с поисковым запросом в Яндексе
     * @param query поисковый запрос
     * @throws IOException если ошибка формирования URL или открытия браузера
     */
    private static void openBrowserWithQuery(String query) throws IOException {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
            String searchUrl = "https://yandex.ru/search/?text=" + encodedQuery;
            URI uri = new URI(searchUrl);
            Desktop.getDesktop().browse(uri);
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Некорректный URL: " + e.getMessage(), e);
        }
    }
}
