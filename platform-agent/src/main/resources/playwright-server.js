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
const config = require('./playwright-server.config.js');

const app = express();
app.use(express.json());

const SCREENSHOTS_DIR = config.paths.screenshotsDir;

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

/** Задержка между действиями (мс), масштабируется actionDelayMultiplier из конфига */
function delayMs(min, max) {
    return Math.round(randomBetween(min, max + 1) * config.delays.actionDelayMultiplier);
}

// Easing функция для плавного движения
function easeInOutCubic(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
}

function buildHumanPath(fromX, fromY, toX, toY, steps) {
    const mp = config.mousePath;
    const dx = toX - fromX;
    const dy = toY - fromY;
    const distance = Math.hypot(dx, dy);
    const controlOffset = Math.min(
        mp.controlOffsetMax,
        Math.max(mp.controlOffsetMin, distance * mp.distanceFractionForControl)
    );
    const cp1x = fromX + dx * mp.cp1xAlong + randomBetween(-controlOffset, controlOffset);
    const cp1y = fromY + dy * mp.cp1yAlong + randomBetween(-controlOffset, controlOffset);
    const cp2x = fromX + dx * mp.cp2xAlong + randomBetween(-controlOffset, controlOffset);
    const cp2y = fromY + dy * mp.cp2yAlong + randomBetween(-controlOffset, controlOffset);
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
    const ci = config.cursorInit;
    const vb = config.browser;
    const viewport = page.viewportSize() || { width: vb.viewportWidth, height: vb.viewportHeight };
    const startX = randomBetween(viewport.width * ci.startXMinFrac, viewport.width * ci.startXMaxFrac);
    const startY = randomBetween(viewport.height * ci.startYMinFrac, viewport.height * ci.startYMaxFrac);
    await page.mouse.move(startX, startY);
    cursorState = { x: startX, y: startY, initialized: true };
}

// Плавное движение курсора с легкой рандомизацией траектории и скорости.
async function smoothMove(page, toX, toY, options = {}) {
    await ensureCursorInitialized(page);
    const fromX = cursorState.x;
    const fromY = cursorState.y;
    const sm = config.smoothMove;
    const distance = Math.hypot(toX - fromX, toY - fromY);
    const steps = options.steps || Math.max(
        sm.minSteps,
        Math.min(sm.maxSteps, Math.round(distance / randomBetween(sm.distanceDivisorMin, sm.distanceDivisorMax)))
    );
    const points = buildHumanPath(fromX, fromY, toX, toY, steps);

    for (const point of points) {
        await page.mouse.move(point.x, point.y);
        await page.waitForTimeout(delayMs(config.delays.smoothMovePointMinMs, config.delays.smoothMovePointMaxMs));
    }

    cursorState = { x: toX, y: toY, initialized: true };
}

// Подсветка элемента
async function highlightElement(page, selector) {
    await page.evaluate(
        ({ sel, styleCss, restoreMs }) => {
            const element = document.querySelector(sel);
            if (element) {
                const originalStyle = element.style.cssText;
                element.style.cssText = styleCss;
                setTimeout(() => {
                    element.style.cssText = originalStyle;
                }, restoreMs);
            }
        },
        {
            sel: selector,
            styleCss: config.highlight.elementStyleCss,
            restoreMs: config.timeouts.highlightRestoreMs,
        }
    );
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
        const { baseUrl: url, headless = config.server.headless } = req.body;
        baseUrl = url || config.browser.defaultBaseUrl;

        if (browser) {
            await browser.close();
        }

        browser = await chromium.launch({
            headless: headless,
        });

        context = await browser.newContext({
            viewport: {
                width: config.browser.viewportWidth,
                height: config.browser.viewportHeight,
            },
        });

        const initPayload = { injectUi: config.injectUi };

        /** На всех страницах контекста: только оверлей курсора; чат — в Electron (chat-overlay). */
        await context.addInitScript((payload) => {
            const { injectUi } = payload;
            const cu = injectUi.cursor;
            const cursor = document.createElement('div');
            cursor.id = cu.id;
            Object.assign(cursor.style, cu.style);
            document.addEventListener('mousemove', e => {
                cursor.style.left = e.clientX + 'px';
                cursor.style.top = e.clientY + 'px';
            });
            document.addEventListener('mousedown', () => {
                Object.assign(cursor.style, cu.mousedown);
            });
            document.addEventListener('mouseup', () => {
                Object.assign(cursor.style, cu.mouseup);
            });

            document.addEventListener('DOMContentLoaded', () => {
                const legacyChat = document.getElementById('__ap_chat_panel');
                if (legacyChat) {
                    legacyChat.remove();
                }
                document.body.appendChild(cursor);
            });
        }, initPayload);

        page = await context.newPage();
        cursorState = { x: 0, y: 0, initialized: false };

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
                if (config.delays.openPageDelayMs > 0) {
                    await page.waitForTimeout(config.delays.openPageDelayMs);
                }
                result = { url };
                break;
            }

            case 'CLICK':
                await page.waitForSelector(target, { timeout: config.timeouts.selectorMs });
                await highlightElement(page, target);
                const coords = await getElementCoordinates(page, target);
                await smoothMove(page, coords.x, coords.y);
                if (config.delays.pauseBeforeClickMs > 0) {
                    await page.waitForTimeout(config.delays.pauseBeforeClickMs);
                } else {
                    await page.waitForTimeout(delayMs(config.delays.clickFallbackMinMs, config.delays.clickFallbackMaxMs));
                }
                await page.mouse.down();
                await page.waitForTimeout(delayMs(config.delays.mouseDownUpMinMs, config.delays.mouseDownUpMaxMs));
                await page.mouse.up();
                result = mergeCoordinates({ selector: target, selectorUsed: target }, coords);
                break;

            case 'HOVER':
                await page.waitForSelector(target, { timeout: config.timeouts.selectorMs });
                await highlightElement(page, target);
                const hoverCoords = await getElementCoordinates(page, target);
                await smoothMove(page, hoverCoords.x, hoverCoords.y);
                result = mergeCoordinates({ selector: target, selectorUsed: target }, hoverCoords);
                break;

            case 'TYPE':
                await page.waitForSelector(target, { timeout: config.timeouts.selectorMs });
                await highlightElement(page, target);
                const typeCoords = await getElementCoordinates(page, target);
                await smoothMove(page, typeCoords.x, typeCoords.y);
                if (config.delays.pauseBeforeClickMs > 0) {
                    await page.waitForTimeout(config.delays.pauseBeforeClickMs);
                } else {
                    await page.waitForTimeout(delayMs(config.delays.typeFocusFallbackMinMs, config.delays.typeFocusFallbackMaxMs));
                }
                await page.mouse.click(typeCoords.x, typeCoords.y, {
                    delay: delayMs(config.delays.typeMouseClickMinMs, config.delays.typeMouseClickMaxMs),
                });
                await page.keyboard.press('ControlOrMeta+A');
                await page.keyboard.press('Backspace');
                await page.keyboard.type(parameters.text || '', {
                    delay: delayMs(config.delays.typeKeypressMinMs, config.delays.typeKeypressMaxMs),
                });
                if (parameters.pressEnter) {
                    await page.waitForTimeout(delayMs(config.delays.afterTypeEnterMinMs, config.delays.afterTypeEnterMaxMs));
                    await page.keyboard.press('Enter');
                }
                result = mergeCoordinates({ selector: target, selectorUsed: target, text: parameters.text }, typeCoords);
                break;

            case 'WAIT':
                const timeout = parameters.timeout || config.timeouts.waitDefaultMs;
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
                await page.waitForSelector(target, { timeout: config.timeouts.selectorMs });
                await highlightElement(page, target);
                result = { selector: target };
                break;

            case 'SCREENSHOT':
                const screenshotPath = path.join(SCREENSHOTS_DIR, `screenshot-${Date.now()}.png`);
                await page.screenshot({ path: screenshotPath, fullPage: false });
                result = { screenshot: screenshotPath };
                break;

            case 'RESOLVE_COORDS':
                await page.waitForSelector(target, { timeout: config.timeouts.selectorMs });
                const resolvedCoords = await getElementCoordinates(page, target);
                result = mergeCoordinates({ selector: target, selectorUsed: target }, resolvedCoords);
                break;

            case 'CLICK_AT':
                if (parameters.x === undefined || parameters.y === undefined) {
                    throw new Error('CLICK_AT requires numeric x and y parameters');
                }
                await smoothMove(page, Number(parameters.x), Number(parameters.y));
                if (config.delays.pauseBeforeClickMs > 0) {
                    await page.waitForTimeout(config.delays.pauseBeforeClickMs);
                } else {
                    await page.waitForTimeout(delayMs(config.delays.clickFallbackMinMs, config.delays.clickFallbackMaxMs));
                }
                await page.mouse.down();
                await page.waitForTimeout(delayMs(config.delays.mouseDownUpMinMs, config.delays.mouseDownUpMaxMs));
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
            await page.waitForTimeout(delayMs(config.delays.errorScreenshotDelayMinMs, config.delays.errorScreenshotDelayMaxMs));
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

app.listen(config.server.port, () => {
    console.log(`Playwright Server running on http://localhost:${config.server.port}`);
    console.log(`Headless mode: ${config.server.headless}`);
    console.log(`ACTION_DELAY_MULTIPLIER: ${config.delays.actionDelayMultiplier}`);
    if (config.delays.openPageDelayMs > 0) {
        console.log(`OPEN_PAGE_DELAY_MS: ${config.delays.openPageDelayMs}`);
    }
    if (config.delays.pauseBeforeClickMs > 0) {
        console.log(`PAUSE_BEFORE_CLICK_MS: ${config.delays.pauseBeforeClickMs}`);
    }
    console.log(`Screenshots directory: ${SCREENSHOTS_DIR}`);
});



