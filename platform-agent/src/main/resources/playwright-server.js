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
const ACTION_DELAY_MULTIPLIER = Math.max(1, parseInt(process.env.ACTION_DELAY_MULTIPLIER || '1', 10));
/** Задержка (мс) после открытия страницы (только OPEN_PAGE); 0 = без задержки */
const OPEN_PAGE_DELAY_MS = Math.max(0, parseInt(process.env.OPEN_PAGE_DELAY_MS || '0', 10));
/** Пауза (мс) после наведения перед кликом: навёлся → подождал → нажал. По умолчанию 1000. */
const PAUSE_BEFORE_CLICK_MS = Math.max(0, parseInt(process.env.PAUSE_BEFORE_CLICK_MS || '1000', 10));

// Создаем директорию для скриншотов
if (!fs.existsSync(SCREENSHOTS_DIR)) {
    fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

let browser = null;
let context = null;
let page = null;
let baseUrl = null;
let cursorState = { x: 0, y: 0, initialized: false };

function randomBetween(min, max) {
    return min + Math.random() * (max - min);
}

function randomInt(min, max) {
    return Math.floor(randomBetween(min, max + 1));
}

/** Задержка между действиями (мс), масштабируется ACTION_DELAY_MULTIPLIER */
function delayMs(min, max) {
    return Math.round(randomBetween(min, max + 1) * ACTION_DELAY_MULTIPLIER);
}

// Easing функция для плавного движения
function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
}

function buildHumanPath(fromX, fromY, toX, toY, steps) {
    const dx = toX - fromX;
    const dy = toY - fromY;
    const distance = Math.hypot(dx, dy);
    const controlOffset = Math.min(60, Math.max(10, distance * 0.15));
    const cp1x = fromX + dx * 0.35 + randomBetween(-controlOffset, controlOffset);
    const cp1y = fromY + dy * 0.25 + randomBetween(-controlOffset, controlOffset);
    const cp2x = fromX + dx * 0.75 + randomBetween(-controlOffset, controlOffset);
    const cp2y = fromY + dy * 0.65 + randomBetween(-controlOffset, controlOffset);
    const points = [];

    for (let i = 0; i <= steps; i++) {
        const t = easeInOutCubic(i / steps);
        const inv = 1 - t;
        const x = inv ** 3 * fromX
            + 3 * inv ** 2 * t * cp1x
            + 3 * inv * t ** 2 * cp2x
            + t ** 3 * toX;
        const y = inv ** 3 * fromY
            + 3 * inv ** 2 * t * cp1y
            + 3 * inv * t ** 2 * cp2y
            + t ** 3 * toY;
        points.push({ x, y });
    }

    return points;
}

async function ensureCursorInitialized(page) {
    if (cursorState.initialized) {
        return;
    }
    const viewport = page.viewportSize() || { width: 1920, height: 1080 };
    const startX = randomBetween(viewport.width * 0.3, viewport.width * 0.7);
    const startY = randomBetween(viewport.height * 0.2, viewport.height * 0.8);
    await page.mouse.move(startX, startY);
    cursorState = { x: startX, y: startY, initialized: true };
}

// Плавное движение курсора с легкой рандомизацией траектории и скорости.
async function smoothMove(page, toX, toY, options = {}) {
    await ensureCursorInitialized(page);
    const fromX = cursorState.x;
    const fromY = cursorState.y;
    const distance = Math.hypot(toX - fromX, toY - fromY);
    const steps = options.steps || Math.max(12, Math.min(60, Math.round(distance / randomBetween(7, 11))));
    const points = buildHumanPath(fromX, fromY, toX, toY, steps);

    for (const point of points) {
        await page.mouse.move(point.x, point.y);
        await page.waitForTimeout(delayMs(6, 16));
    }

    cursorState = { x: toX, y: toY, initialized: true };
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
    const locator = page.locator(selector).first();
    await locator.scrollIntoViewIfNeeded();
    const box = await locator.boundingBox();
    if (!box) {
        throw new Error(`Element not found: ${selector}`);
    }
    return {
        x: box.x + box.width / 2,
        y: box.y + box.height / 2,
        width: box.width,
        height: box.height,
        left: box.x,
        top: box.y,
        right: box.x + box.width,
        bottom: box.y + box.height
    };
}

function mergeCoordinates(target, coords) {
    if (!coords) {
        return target;
    }
    target.coordinates = coords;
    target.x = coords.x;
    target.y = coords.y;
    target.width = coords.width;
    target.height = coords.height;
    return target;
}

async function captureStepScreenshot(page, prefix = 'step') {
    const screenshotPath = path.join(SCREENSHOTS_DIR, `${prefix}-${Date.now()}.png`);
    await page.screenshot({ path: screenshotPath, fullPage: false });
    return screenshotPath;
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
        });

        context = await browser.newContext({
            viewport: { width: 1920, height: 1080 },
        });

        page = await context.newPage();
        cursorState = { x: 0, y: 0, initialized: false };

        await page.addInitScript(() => {
            const cursor = document.createElement('div');
            cursor.id = '__pw_cursor';
            Object.assign(cursor.style, {
                position: 'fixed', zIndex: '2147483647', pointerEvents: 'none',
                width: '20px', height: '20px', borderRadius: '50%',
                background: 'rgba(255, 50, 50, 0.7)', border: '2px solid #fff',
                boxShadow: '0 0 8px rgba(255,50,50,0.5)',
                transform: 'translate(-50%, -50%)', transition: 'left 0.03s, top 0.03s',
                left: '-100px', top: '-100px',
            });
            document.addEventListener('DOMContentLoaded', () => document.body.appendChild(cursor));
            document.addEventListener('mousemove', e => {
                cursor.style.left = e.clientX + 'px';
                cursor.style.top  = e.clientY + 'px';
            });
            document.addEventListener('mousedown', () => {
                cursor.style.transform = 'translate(-50%, -50%) scale(0.7)';
                cursor.style.background = 'rgba(255, 0, 0, 0.9)';
            });
            document.addEventListener('mouseup', () => {
                cursor.style.transform = 'translate(-50%, -50%) scale(1)';
                cursor.style.background = 'rgba(255, 50, 50, 0.7)';
            });
        });

        res.json({
            success: true,
            message: 'Browser initialized',
            data: { baseUrl, headless },
            executionTimeMs: 0
        });
    } catch (error) {
        console.error('[INIT ERROR]', error.message, error.stack);
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
            case 'OPEN_PAGE': {
                const url = target.startsWith('http') ? target : `${baseUrl}${target}`;
                await page.goto(url, { waitUntil: 'domcontentloaded' });
                if (OPEN_PAGE_DELAY_MS > 0) {
                    await page.waitForTimeout(OPEN_PAGE_DELAY_MS);
                }
                result = { url };
                break;
            }

            case 'CLICK':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                const coords = await getElementCoordinates(page, target);
                await smoothMove(page, coords.x, coords.y);
                if (PAUSE_BEFORE_CLICK_MS > 0) {
                    await page.waitForTimeout(PAUSE_BEFORE_CLICK_MS);
                } else {
                    await page.waitForTimeout(delayMs(80, 180));
                }
                await page.mouse.down();
                await page.waitForTimeout(delayMs(35, 90));
                await page.mouse.up();
                result = mergeCoordinates({ selector: target, selectorUsed: target }, coords);
                break;

            case 'HOVER':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                const hoverCoords = await getElementCoordinates(page, target);
                await smoothMove(page, hoverCoords.x, hoverCoords.y);
                result = mergeCoordinates({ selector: target, selectorUsed: target }, hoverCoords);
                break;

            case 'TYPE':
                await page.waitForSelector(target, { timeout: 10000 });
                await highlightElement(page, target);
                const typeCoords = await getElementCoordinates(page, target);
                await smoothMove(page, typeCoords.x, typeCoords.y);
                if (PAUSE_BEFORE_CLICK_MS > 0) {
                    await page.waitForTimeout(PAUSE_BEFORE_CLICK_MS);
                } else {
                    await page.waitForTimeout(delayMs(70, 170));
                }
                await page.mouse.click(typeCoords.x, typeCoords.y, { delay: delayMs(30, 80) });
                await page.keyboard.press('ControlOrMeta+A');
                await page.keyboard.press('Backspace');
                await page.keyboard.type(parameters.text || '', { delay: delayMs(40, 120) });
                if (parameters.pressEnter) {
                    await page.waitForTimeout(delayMs(200, 400));
                    await page.keyboard.press('Enter');
                }
                result = mergeCoordinates({ selector: target, selectorUsed: target, text: parameters.text }, typeCoords);
                break;

            case 'WAIT':
                const timeout = parameters.timeout || 10000;
                const condition = target || 'domcontentloaded';
                if (condition === 'networkidle' || condition === 'domcontentloaded' || condition === 'load') {
                    await page.waitForLoadState(condition, { timeout });
                } else if (condition === 'result') {
                    await page.waitForLoadState('domcontentloaded', { timeout });
                } else {
                    await page.waitForSelector(condition, { timeout, state: 'visible' });
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
                await page.screenshot({ path: screenshotPath, fullPage: false });
                result = { screenshot: screenshotPath };
                break;

            case 'RESOLVE_COORDS':
                await page.waitForSelector(target, { timeout: 10000 });
                const resolvedCoords = await getElementCoordinates(page, target);
                result = mergeCoordinates({ selector: target, selectorUsed: target }, resolvedCoords);
                break;

            case 'CLICK_AT':
                if (parameters.x === undefined || parameters.y === undefined) {
                    throw new Error('CLICK_AT requires numeric x and y parameters');
                }
                await smoothMove(page, Number(parameters.x), Number(parameters.y));
                if (PAUSE_BEFORE_CLICK_MS > 0) {
                    await page.waitForTimeout(PAUSE_BEFORE_CLICK_MS);
                } else {
                    await page.waitForTimeout(delayMs(80, 180));
                }
                await page.mouse.down();
                await page.waitForTimeout(delayMs(35, 90));
                await page.mouse.up();
                result = {
                    x: Number(parameters.x),
                    y: Number(parameters.y),
                    button: parameters.button || 'left',
                    selectorUsed: parameters.selectorUsed || target || null
                };
                break;

            default:
                throw new Error(`Unknown command type: ${type}`);
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
        let errorScreenshot = null;
        try {
            await page.waitForTimeout(delayMs(200, 400));
            errorScreenshot = await captureStepScreenshot(page, 'error');
        } catch (ignored) {}
        
        res.status(500).json({
            success: false,
            error: error.message,
            data: errorScreenshot ? { screenshot: errorScreenshot } : {},
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
        cursorState = { x: 0, y: 0, initialized: false };

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
    console.log(`ACTION_DELAY_MULTIPLIER: ${ACTION_DELAY_MULTIPLIER}`);
    if (OPEN_PAGE_DELAY_MS > 0) {
        console.log(`OPEN_PAGE_DELAY_MS: ${OPEN_PAGE_DELAY_MS}`);
    }
    if (PAUSE_BEFORE_CLICK_MS > 0) {
        console.log(`PAUSE_BEFORE_CLICK_MS: ${PAUSE_BEFORE_CLICK_MS}`);
    }
    console.log(`Screenshots directory: ${SCREENSHOTS_DIR}`);
});



