/**
 * Playwright Server для UI-агента.
 * Предоставляет HTTP API для управления браузером через Playwright.
 * 
 * Запуск: node playwright-server.js [--port 3000] [--headless]
 */

const { chromium } = require('playwright');
const express = require('express');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(express.json());

const PORT = process.env.PORT || 3000;
const HEADLESS = process.env.HEADLESS === 'true' || process.argv.includes('--headless');
const SCREENSHOTS_DIR = process.env.SCREENSHOTS_DIR || path.join(__dirname, 'screenshots');

// Создаем директорию для скриншотов
if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

let browser = null;
let context = null;
let page = null;
let baseUrl = null;

// Утилита для плавного движения мыши
async function smoothMove(page, fromX, fromY, toX, toY, steps = 20) {
    for (let i = 0; i <= steps; i++) {
        const progress = i / steps;
        const easeProgress = easeInOutCubic(progress);
        const x = fromX + (toX - fromX) * easeProgress;
        const y = fromY + (toY - fromY) * easeProgress;
        await page.mouse.move(x, y);
        await page.waitForTimeout(10);
    }
}

// Easing функция для плавного движения
function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
}

// Подсветка элемента
async function highlightElement(page, selector) {
    await page.evaluate((sel) => {
        const element = document.querySelector(sel);
        if (element) {
            const originalStyle = element.style.cssText;
            element.style.cssText = 'outline: 3px solid #ff6b6b !important; outline-offset: 2px !important; background-color: rgba(255, 107, 107, 0.1) !important;';
            setTimeout(() => {
                element.style.cssText = originalStyle;
            }, 1000);
        }
    }, selector);
}

// Получение координат элемента
async function getElementCoordinates(page, selector) {
    const box = await page.locator(selector).boundingBox();
    if (!box) {
        throw new Error(`Element not found: ${selector}`);
    }
    return {
        x: box.x + box.width / 2,
        y: box.y + box.height / 2
    };
}

// Health check
app.get('/health', (req, res) => {
    res.json({ status: 'ok', browser: browser !== null });
});

// Инициализация браузера
app.post('/initialize', async (req, res) => {
    try {
        const { baseUrl: url, headless = HEADLESS } = req.body;
        baseUrl = url || 'http://localhost:8080';

        if (browser) {
            await browser.close();
        }

        browser = await chromium.launch({
            headless: headless,
            slowMo: 100, // Замедление для визуализации
        });

        context = await browser.newContext({
            viewport: { width: 1920, height: 1080 },
            recordVideo: {
                dir: path.join(SCREENSHOTS_DIR, 'videos'),
                size: { width: 1920, height: 1080 }
            }
        });

        page = await context.newPage();
        await page.goto(baseUrl);

        res.json({
            success: true,
            message: 'Browser initialized',
            data: { baseUrl, headless },
            executionTimeMs: 0
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message,
            executionTimeMs: 0
        });
    }
});

// Выполнение команды
app.post('/execute', async (req, res) => {
    const startTime = Date.now();
    
    if (!page) {
        return res.status(400).json({
            success: false,
            error: 'Browser not initialized. Call /initialize first.',
            executionTimeMs: Date.now() - startTime
        });
    }

    const { type, target, explanation, parameters = {} } = req.body;

    try {
        let result = {};

        switch (type) {
            case 'OPEN_PAGE':
                const url = target.startsWith('http') ? target : `${baseUrl}${target}`;
                await page.goto(url, { waitUntil: 'networkidle' });
                result = { url };
                break;

            case 'CLICK':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                const coords = await getElementCoordinates(page, target);
                await smoothMove(page, 0, 0, coords.x, coords.y);
                await page.waitForTimeout(200);
                await page.click(target);
                result = { selector: target };
                break;

            case 'HOVER':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                const hoverCoords = await getElementCoordinates(page, target);
                await smoothMove(page, 0, 0, hoverCoords.x, hoverCoords.y);
                await page.hover(target);
                result = { selector: target };
                break;

            case 'TYPE':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                await page.fill(target, parameters.text || '');
                result = { selector: target, text: parameters.text };
                break;

            case 'WAIT':
                const timeout = parameters.timeout || 5000;
                const condition = target || 'networkidle';
                if (condition === 'result' || condition === 'networkidle') {
                    await page.waitForLoadState('networkidle', { timeout });
                } else {
                    await page.waitForSelector(condition, { timeout });
                }
                result = { condition, timeout };
                break;

            case 'EXPLAIN':
                // Логируем объяснение
                console.log(`[EXPLAIN] ${explanation}`);
                result = { message: explanation };
                break;

            case 'HIGHLIGHT':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                result = { selector: target };
                break;

            case 'SCREENSHOT':
                const screenshotPath = path.join(SCREENSHOTS_DIR, `screenshot-${Date.now()}.png`);
                await page.screenshot({ path: screenshotPath, fullPage: true });
                result = { screenshot: screenshotPath };
                break;

            default:
                throw new Error(`Unknown command type: ${type}`);
        }

        // Делаем скриншот после каждого действия (опционально)
        if (type !== 'SCREENSHOT' && type !== 'EXPLAIN') {
            const screenshotPath = path.join(SCREENSHOTS_DIR, `step-${Date.now()}.png`);
            await page.screenshot({ path: screenshotPath, fullPage: false });
            result.screenshot = screenshotPath;
        }

        const executionTime = Date.now() - startTime;
        
        res.json({
            success: true,
            message: explanation || `Command ${type} executed successfully`,
            data: result,
            executionTimeMs: executionTime
        });

    } catch (error) {
        const executionTime = Date.now() - startTime;
        console.error(`[ERROR] Command ${type} failed:`, error);
        
        res.status(500).json({
            success: false,
            error: error.message,
            executionTimeMs: executionTime
        });
    }
});

// Закрытие браузера
app.post('/close', async (req, res) => {
    try {
        if (context) {
            await context.close();
            context = null;
        }
        if (browser) {
            await browser.close();
            browser = null;
        }
        page = null;

        res.json({
            success: true,
            message: 'Browser closed',
            executionTimeMs: 0
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message,
            executionTimeMs: 0
        });
    }
});

// Graceful shutdown
process.on('SIGINT', async () => {
    console.log('\nShutting down...');
    if (browser) {
        await browser.close();
    }
    process.exit(0);
});

app.listen(PORT, () => {
    console.log(`Playwright Server running on http://localhost:${PORT}`);
    console.log(`Headless mode: ${HEADLESS}`);
    console.log(`Screenshots directory: ${SCREENSHOTS_DIR}`);
});

