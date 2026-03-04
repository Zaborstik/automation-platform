package com.zaborstik.platform.agent.ui;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class AgentDesktopApp {

    // Приблизительная высота панели вкладок/адресной строки браузера (в пикселях).
    // Эту константу можно подправить под конкретное рабочее место,
    // если курсор попадает немного выше/ниже ссылки.
    private static final int BROWSER_UI_HEIGHT_OFFSET = 110;

    private enum BrowserType {
        AUTO,
        CHROME,
        YANDEX,
        OPERA,
        SAFARI
    }

    private static JTextArea resultArea;
    private static JTextField queryField;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AgentDesktopApp::createAndShowUI);
    }

    private static void createAndShowUI() {
        JFrame frame = new JFrame("Automation Platform – Браузерный агент");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(600, 400));

        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        JLabel promptLabel = new JLabel("Введите запрос:");
        queryField = new JTextField();
        inputPanel.add(promptLabel, BorderLayout.WEST);
        inputPanel.add(queryField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Скрыть окна, открыть поиск и навести курсор");

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

                // Спросим у пользователя, в каком браузере открыть поиск.
                BrowserType browserType = askUserForBrowser();
                if (browserType == null) {
                    resultArea.append("Операция отменена пользователем.\n");
                    return;
                }

                resultArea.append("Отправлен запрос: " + query + "\n");

                final BrowserType finalBrowserType = browserType;
                new Thread(() -> runSearchAndMoveCursor(query, finalBrowserType)).start();
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static boolean isMacOS() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("mac") || os.contains("osx");
    }

    private static void hideAllWindows() throws AWTException {
        Robot robot = new Robot();

        if (isMacOS()) {
            robot.keyPress(KeyEvent.VK_META);
            robot.keyPress(KeyEvent.VK_H);
            robot.delay(50);
            robot.keyRelease(KeyEvent.VK_H);
            robot.keyRelease(KeyEvent.VK_META);
        } else {
            robot.keyPress(KeyEvent.VK_WINDOWS);
            robot.keyPress(KeyEvent.VK_D);
            robot.delay(50);
            robot.keyRelease(KeyEvent.VK_D);
            robot.keyRelease(KeyEvent.VK_WINDOWS);
        }
        robot.delay(300);
    }

    private static BrowserType askUserForBrowser() {
        Object[] options = {
                "Авто (Chrome / Yandex / Opera / Safari)",
                "Chrome",
                "Yandex",
                "Opera",
                "Safari"
        };
        Object selected = JOptionPane.showInputDialog(
                null,
                "Выберите браузер для открытия поиска:",
                "Выбор браузера",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (selected == null) {
            return null;
        }
        String choice = selected.toString();
        return switch (choice) {
            case "Chrome" -> BrowserType.CHROME;
            case "Yandex" -> BrowserType.YANDEX;
            case "Opera" -> BrowserType.OPERA;
            case "Safari" -> BrowserType.SAFARI;
            default -> BrowserType.AUTO;
        };
    }

    /**
     * Пробует по очереди запустить Chrome, Yandex, Opera или Safari и возвращает первый рабочий драйвер.
     * Драйверы при необходимости скачиваются через WebDriverManager.
     */
    private static WebDriver createBrowserDriver() {
        return createBrowserDriver(BrowserType.AUTO);
    }

    private static WebDriver createBrowserDriver(BrowserType browserType) {
        // 1) Chrome
        if (browserType == BrowserType.CHROME || browserType == BrowserType.AUTO) {
            try {
                WebDriverManager.chromedriver().setup();
                ChromeOptions opts = new ChromeOptions();
                // Стараемся использовать русскую локаль, чтобы результаты поиска были ближе к обычным
                opts.addArguments("--lang=ru");
                WebDriver d = new ChromeDriver(opts);
                SwingUtilities.invokeLater(() -> resultArea.append("Используется браузер: Chrome.\n"));
                return d;
            } catch (Exception e) {
                if (browserType == BrowserType.CHROME) {
                    throw new IllegalStateException("Не удалось запустить Chrome через WebDriver.", e);
                }
                // пробуем следующий
            }
        }

        // 2) Yandex (Chromium, тот же ChromeDriver, другой исполняемый файл)
        if (browserType == BrowserType.YANDEX || browserType == BrowserType.AUTO) {
            String yandexPath = findYandexBrowserPath();
            if (yandexPath != null) {
                try {
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions opts = new ChromeOptions();
                    opts.addArguments("--lang=ru");
                    opts.setBinary(yandexPath);
                    WebDriver d = new ChromeDriver(opts);
                    SwingUtilities.invokeLater(() -> resultArea.append("Используется браузер: Yandex.\n"));
                    return d;
                } catch (Exception e) {
                    if (browserType == BrowserType.YANDEX) {
                        throw new IllegalStateException("Не удалось запустить Yandex Browser через WebDriver.", e);
                    }
                    // пробуем следующий
                }
            } else if (browserType == BrowserType.YANDEX) {
                throw new IllegalStateException("Yandex Browser не найден на этом компьютере.");
            }
        }

        // 3) Opera (Chromium, можно использовать тот же ChromeDriver с другим бинарником)
        if (browserType == BrowserType.OPERA || browserType == BrowserType.AUTO) {
            String operaPath = findOperaBrowserPath();
            if (operaPath != null) {
                try {
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions opts = new ChromeOptions();
                    opts.addArguments("--lang=ru");
                    opts.setBinary(operaPath);
                    WebDriver d = new ChromeDriver(opts);
                    SwingUtilities.invokeLater(() -> resultArea.append("Используется браузер: Opera.\n"));
                    return d;
                } catch (Exception e) {
                    if (browserType == BrowserType.OPERA) {
                        throw new IllegalStateException("Не удалось запустить Opera через WebDriver.", e);
                    }
                    // пробуем следующий
                }
            } else if (browserType == BrowserType.OPERA) {
                throw new IllegalStateException("Opera не найдена на этом компьютере.");
            }
        }

        // 4) Safari (только macOS, в настройках Safari нужно включить «Разработка → Разрешить удалённую автоматизацию»)
        if ((browserType == BrowserType.SAFARI || browserType == BrowserType.AUTO) && isMacOS()) {
            try {
                WebDriver d = new SafariDriver();
                SwingUtilities.invokeLater(() -> resultArea.append("Используется браузер: Safari.\n"));
                return d;
            } catch (Exception e) {
                if (browserType == BrowserType.SAFARI) {
                    throw new IllegalStateException("Не удалось запустить Safari через WebDriver. Проверьте настройки разработчика.", e);
                }
                // не получилось
            }
        } else if (browserType == BrowserType.SAFARI && !isMacOS()) {
            throw new IllegalStateException("Safari доступен для автоматизации только на macOS.");
        }

        throw new IllegalStateException(
                "Не удалось запустить ни один браузер (Chrome, Yandex, Opera, Safari). " +
                "Установите Chrome или проверьте настройки Safari (macOS).");
    }

    private static String findYandexBrowserPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("ProgramFiles(X86)");
        if (programFiles == null) programFiles = System.getenv("ProgramFiles");
        if (localAppData != null) {
            String p = localAppData + "\\Yandex\\YandexBrowser\\Application\\browser.exe";
            if (new File(p).exists()) return p;
        }
        if (programFiles != null) {
            String p = programFiles + "\\Yandex\\YandexBrowser\\Application\\browser.exe";
            if (new File(p).exists()) return p;
        }
        if (isMacOS()) {
            String p = "/Applications/Yandex.app/Contents/MacOS/Yandex";
            if (new File(p).exists()) return p;
        }
        return null;
    }

    private static String findOperaBrowserPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        String programFiles = System.getenv("ProgramFiles(X86)");
        if (programFiles == null) programFiles = System.getenv("ProgramFiles");
        if (localAppData != null) {
            // Типичный путь установки Opera для текущего пользователя
            String p = localAppData + "\\Programs\\Opera\\opera.exe";
            if (new File(p).exists()) return p;
        }
        if (programFiles != null) {
            // Типичный путь установки Opera для всех пользователей
            String p = programFiles + "\\Opera\\launcher.exe";
            if (new File(p).exists()) return p;
        }
        if (isMacOS()) {
            String p = "/Applications/Opera.app/Contents/MacOS/Opera";
            if (new File(p).exists()) return p;
        }
        return null;
    }

    /**
     * Скрывает все окна, открывает окно браузера с поиском (Bing),
     * находит первый результат и наводит системный курсор мыши
     * на центр первой ссылки по её координатам в окне браузера.
     */
    private static void runSearchAndMoveCursor(String query) {
        runSearchAndMoveCursor(query, BrowserType.AUTO);
    }

    private static void runSearchAndMoveCursor(String query, BrowserType browserType) {
        WebDriver driver = null;
        try {
            hideAllWindows();
            SwingUtilities.invokeLater(() ->
                    resultArea.append("Окна свернуты (Win+D / Cmd+H).\n"));

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            // Добавляем параметры локали, чтобы результаты были ближе к обычному русскому поиску
            String searchUrl = "https://www.bing.com/search?q=" + encodedQuery + "&setlang=ru-ru&cc=ru&mkt=ru-RU";

            driver = createBrowserDriver(browserType);
            driver.manage().window().maximize();
            driver.get(searchUrl);

            String finalSearchUrl = searchUrl;
            SwingUtilities.invokeLater(() ->
                    resultArea.append("Открыта страница результатов поиска (Bing): " + finalSearchUrl + "\n"));

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

            // Ждём, пока появится хотя бы один органический результат
            List<WebElement> candidates = wait.until(d -> {
                // Основной селектор: результаты внутри блока b_results
                List<WebElement> els = d.findElements(By.cssSelector("#b_results li.b_algo h2 a"));
                // Фоллбэк: любые внешние ссылки в блоке результатов
                if (els.isEmpty()) {
                    els = d.findElements(By.cssSelector("#b_results a[href^='http']"));
                }
                return els.isEmpty() ? null : els;
            });

            // Выбираем первую ссылку, которая ведёт НЕ на карты/картинки/внутренние страницы Bing
            WebElement firstLink = null;
            String href = null;
            for (WebElement el : candidates) {
                String h = el.getAttribute("href");
                if (h == null || h.isBlank()) {
                    continue;
                }

                // Фильтруем по видимому тексту табов «Все / Изображения / Видео / Карты / Новости / Ещё / More»
                String text = el.getText();
                if (text != null) {
                    String t = text.trim().toLowerCase();
                    if (t.equals("все") ||
                        t.equals("изображения") ||
                        t.equals("картинки") ||
                        t.equals("видео") ||
                        t.equals("карты") ||
                        t.equals("новости") ||
                        t.equals("ещё") ||
                        t.equals("all") ||
                        t.equals("images") ||
                        t.equals("videos") ||
                        t.equals("maps") ||
                        t.equals("news") ||
                        t.equals("more")) {
                        continue;
                    }
                }

                String lower = h.toLowerCase();

                // Отбрасываем типичные видео-платформы, если они попали в первый блок
                if (lower.contains("youtube.com") ||
                    lower.contains("youtu.be") ||
                    lower.contains("rutube.ru") ||
                    lower.contains("vk.com/video")) {
                    continue;
                }

                if (lower.contains("bing.com/maps") ||
                    lower.contains("/maps") ||
                    lower.contains("bing.com/images") ||
                    lower.contains("/images") ||
                    lower.contains("bing.com/videos") ||
                    lower.contains("/videos") ||
                    lower.startsWith("https://www.bing.com/search")) {
                    // пропускаем вертикали и внутренние ссылки Bing
                    continue;
                }
                firstLink = el;
                href = h;
                break;
            }

            // Если отфильтрованных ссылок нет – падаем обратно к первому результату как есть
            if (firstLink == null) {
                firstLink = candidates.get(0);
                href = firstLink.getAttribute("href");
            }

            Rectangle rect = firstLink.getRect();
            Point windowPos = driver.manage().window().getPosition();

            // Смещаем точку немного левее центра, чтобы гарантированно попадать на текст,
            // даже если ссылка короткая или рядом есть иконка.
            int targetX = windowPos.getX() + rect.getX() + rect.getWidth() / 2 - 15;
            int targetY = windowPos.getY() + BROWSER_UI_HEIGHT_OFFSET + rect.getY() + rect.getHeight() / 2;

            Robot robot = new Robot();

            // Плавное перемещение курсора от текущей позиции к цели
            java.awt.Point currentPos = MouseInfo.getPointerInfo().getLocation();
            int startX = currentPos.x;
            int startY = currentPos.y;
            int steps = 50;
            for (int i = 1; i <= steps; i++) {
                int x = startX + (targetX - startX) * i / steps;
                int y = startY + (targetY - startY) * i / steps;
                robot.mouseMove(x, y);
                robot.delay(20); // ~1 секунда суммарно
            }

            // Ждём 3 секунды после наведения и кликаем
            robot.delay(3000);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

            final String finalHref = href;
            final int finalTargetX = targetX;
            final int finalTargetY = targetY;

            SwingUtilities.invokeLater(() -> {
                resultArea.append("Плавно навёл курсор и кликнул по первой ссылке.\n");
                if (finalHref != null) {
                    resultArea.append("URL первой ссылки: " + finalHref + "\n");
                }
                resultArea.append("Координаты курсора: x=" + finalTargetX + ", y=" + finalTargetY + "\n");
                queryField.setText("");
            });

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    resultArea.append("Ошибка при поиске и наведении курсора: " + ex.getMessage() + "\n"));
        }
        // Не закрываем браузер специально, чтобы пользователь видел результат.
    }
}
