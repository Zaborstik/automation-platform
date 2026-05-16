/**
 * Конфигурация Playwright-сервера.
 *
 * Секции:
 * - server — порт и headless;
 * - paths — каталог скриншотов;
 * - delays — все задержки в миллисекундах (см. комментарии у каждого поля);
 * - timeouts — таймауты ожидания селекторов и команд;
 * - browser — базовый URL и размер окна;
 * - mousePath — параметры изогнутой траектории мыши (Безье);
 * - smoothMove — число шагов движения к цели;
 * - cursorInit — случайная стартовая позиция курсора в viewport;
 * - highlight — стиль подсветки DOM-элемента;
 * - injectUi — стили оверлея курсора в автоматизируемой странице (чат — в Electron, см. chat-overlay).
 *
 * Переменные окружения переопределяют значения там, где это явно указано в комментариях.
 */
'use strict';

const path = require('path');

function intEnv(name, def, { min, max } = {}) {
    const raw = process.env[name];
    if (raw === undefined || raw === '') {
        return def;
    }
    const n = parseInt(raw, 10);
    if (Number.isNaN(n)) {
        return def;
    }
    let v = n;
    if (min !== undefined) {
        v = Math.max(min, v);
    }
    if (max !== undefined) {
        v = Math.min(max, v);
    }
    return v;
}

function buildConfig() {
    const rootDir = __dirname;

    return {
        server: {
            /** Порт HTTP-сервера агента; переопределение: переменная PORT */
            port: process.env.PORT || 3000,
            /** Запуск браузера без окна: HEADLESS=true или аргумент --headless */
            headless:
                process.env.HEADLESS === 'true' || process.argv.includes('--headless'),
        },

        paths: {
            /** Каталог для скриншотов шагов и ошибок; переопределение: SCREENSHOTS_DIR */
            screenshotsDir:
                process.env.SCREENSHOTS_DIR || path.join(rootDir, 'screenshots'),
        },

        delays: {
            /**
             * Общий множитель для «человеческих» случайных задержек (delayMs).
             * Увеличивает все интервалы между движениями мыши, нажатиями и вводом.
             * Переопределение: ACTION_DELAY_MULTIPLIER (минимум 1).
             */
            actionDelayMultiplier: intEnv('ACTION_DELAY_MULTIPLIER', 1, { min: 1 }),
            /**
             * Пауза после OPEN_PAGE, когда страница уже загружена (дом готов).
             * 0 — не ждать. Переопределение: OPEN_PAGE_DELAY_MS.
             */
            openPageDelayMs: intEnv('OPEN_PAGE_DELAY_MS', 0, { min: 0 }),
            /**
             * Пауза после наведения курсора на цель и перед кликом (имитация «подумал — нажал»).
             * 0 — вместо неё используются короткие случайные fallback-задержки (см. clickFallback*).
             * Переопределение: PAUSE_BEFORE_CLICK_MS.
             */
            pauseBeforeClickMs: intEnv('PAUSE_BEFORE_CLICK_MS', 1000, { min: 0 }),
            /**
             * Минимальная задержка (мс) между соседними точками при плавном движении мыши (smoothMove).
             */
            smoothMovePointMinMs: 6,
            /**
             * Максимальная задержка (мс) между соседними точками траектории smoothMove (случайное значение в диапазоне).
             */
            smoothMovePointMaxMs: 16,
            /**
             * Минимальная пауза (мс) между mouse.down и mouse.up при клике (CLICK, CLICK_AT).
             */
            mouseDownUpMinMs: 35,
            /**
             * Максимальная пауза (мс) между mouse.down и mouse.up при клике.
             */
            mouseDownUpMaxMs: 90,
            /**
             * Минимальная задержка (мс) перед кликом, если pauseBeforeClickMs = 0 (CLICK, CLICK_AT).
             */
            clickFallbackMinMs: 80,
            /**
             * Максимальная задержка (мс) перед кликом при pauseBeforeClickMs = 0.
             */
            clickFallbackMaxMs: 180,
            /**
             * Минимальная задержка (мс) перед кликом по полю ввода при pauseBeforeClickMs = 0 (команда TYPE).
             */
            typeFocusFallbackMinMs: 70,
            /**
             * Максимальная задержка (мс) перед кликом по полю ввода при pauseBeforeClickMs = 0.
             */
            typeFocusFallbackMaxMs: 170,
            /**
             * Минимальная длительность (мс) удержания кнопки мыши при клике в поле перед вводом текста (TYPE).
             */
            typeMouseClickMinMs: 30,
            /**
             * Максимальная длительность (мс) клика мышью в поле перед вводом (TYPE).
             */
            typeMouseClickMaxMs: 80,
            /**
             * Минимальная задержка (мс) между нажатиями клавиш при keyboard.type (имитация набора).
             */
            typeKeypressMinMs: 40,
            /**
             * Максимальная задержка (мс) между нажатиями клавиш при наборе текста.
             */
            typeKeypressMaxMs: 120,
            /**
             * Минимальная пауза (мс) после ввода текста, если в команде TYPE передан pressEnter (перед Enter).
             */
            afterTypeEnterMinMs: 200,
            /**
             * Максимальная пауза (мс) после ввода перед нажатием Enter.
             */
            afterTypeEnterMaxMs: 400,
            /**
             * Минимальная задержка (мс) перед снимком экрана при ошибке выполнения команды.
             */
            errorScreenshotDelayMinMs: 200,
            /**
             * Максимальная задержка (мс) перед скриншотом при ошибке.
             */
            errorScreenshotDelayMaxMs: 400,
        },

        timeouts: {
            /** Таймаут ожидания появления элемента по селектору (мс): CLICK, HOVER, TYPE, HIGHLIGHT и т.д. */
            selectorMs: 10000,
            /** Таймаут по умолчанию (мс) для команды WAIT, если в parameters.timeout не передано */
            waitDefaultMs: 10000,
            /** Через сколько миллисекунд снять подсветку с элемента после highlightElement */
            highlightRestoreMs: 1000,
        },

        browser: {
            /** Базовый URL приложения, если в /initialize не передан baseUrl (относительные пути в OPEN_PAGE дописываются к нему) */
            defaultBaseUrl: 'http://localhost:8080',
            /** Ширина окна браузера при создании контекста (пиксели) */
            viewportWidth: 1920,
            /** Высота окна браузера при создании контекста (пиксели) */
            viewportHeight: 1080,
        },

        mousePath: {
            /** Минимальный отклонение контрольных точек Безье от прямой (пиксели) — «кривизна» траектории */
            controlOffsetMin: 10,
            /** Верхняя граница отклонения контрольных точек (пиксели) */
            controlOffsetMax: 60,
            /** Доля длины перемещения, участвующая в расчёте базового отклонения контрольных точек */
            distanceFractionForControl: 0.15,
            /** Доля пути по X для первой контрольной точки кривой (0–1) */
            cp1xAlong: 0.35,
            /** Доля пути по Y для первой контрольной точки */
            cp1yAlong: 0.25,
            /** Доля пути по X для второй контрольной точки */
            cp2xAlong: 0.75,
            /** Доля пути по Y для второй контрольной точки */
            cp2yAlong: 0.65,
        },

        smoothMove: {
            /** Минимальное число шагов интерполяции при движении мыши к цели */
            minSteps: 12,
            /** Максимальное число шагов (зависит от расстояния и делителей ниже) */
            maxSteps: 60,
            /** Нижняя граница делителя расстояния при расчёте числа шагов (больше шагов — плавнее) */
            distanceDivisorMin: 7,
            /** Верхняя граница делителя расстояния (случайное значение между min и max) */
            distanceDivisorMax: 11,
        },

        cursorInit: {
            /** Левая граница случайной стартовой позиции курсора по X (доля ширины viewport, 0–1) */
            startXMinFrac: 0.3,
            /** Правая граница стартовой позиции курсора по X */
            startXMaxFrac: 0.7,
            /** Верхняя граница области старта курсора по Y (доля высоты viewport) */
            startYMinFrac: 0.2,
            /** Нижняя граница области старта курсора по Y */
            startYMaxFrac: 0.8,
        },

        highlight: {
            /** Полная строка cssText элемента на время подсветки перед действием (outline, фон) */
            elementStyleCss:
                'outline: 3px solid #ff6b6b !important; outline-offset: 2px !important; background-color: rgba(255, 107, 107, 0.1) !important;',
        },

        /**
         * Стили для addInitScript: только курсор поверх страницы. Панель чата — отдельное окно platform-agent/chat-overlay.
         */
        injectUi: {
            /** Настройки отображаемого «фейкового» курсора поверх страницы */
            cursor: {
                /** id DOM-элемента курсора */
                id: '__pw_cursor',
                /** Базовый вид курсора (фиксированная позиция, слой выше страницы, без перехвата кликов) */
                style: {
                    position: 'fixed',
                    zIndex: '2147483647',
                    pointerEvents: 'none',
                    width: '20px',
                    height: '20px',
                    borderRadius: '50%',
                    background: 'rgba(255, 50, 50, 0.7)',
                    border: '2px solid #fff',
                    boxShadow: '0 0 8px rgba(255,50,50,0.5)',
                    transform: 'translate(-50%, -50%)',
                    transition: 'left 0.03s, top 0.03s',
                    left: '-100px',
                    top: '-100px',
                },
                /** Вид курсора в момент нажатия кнопки мыши */
                mousedown: {
                    transform: 'translate(-50%, -50%) scale(0.7)',
                    background: 'rgba(255, 0, 0, 0.9)',
                },
                /** Вид курсора после отпускания кнопки */
                mouseup: {
                    transform: 'translate(-50%, -50%) scale(1)',
                    background: 'rgba(255, 50, 50, 0.7)',
                },
            },
        },
    };
}

module.exports = buildConfig();
