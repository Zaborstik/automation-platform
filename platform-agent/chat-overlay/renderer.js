if (!window.__TAURI__?.core) {
    document.body.textContent =
        'Ожидается Tauri (window.__TAURI__). Запускайте через platform-chat-overlay, не открывайте index.html в браузере.';
    throw new Error('Tauri API missing');
}
const { invoke } = window.__TAURI__.core;

const DEFAULT_EXECUTOR_URL = 'http://localhost:7070';
const DEFAULT_POLL_INTERVAL_MS = 1500;

function applyPanelStyleForWindow(panelBase) {
    const s = { ...panelBase };
    s.position = 'relative';
    s.width = '100%';
    s.height = '100%';
    s.maxWidth = '100%';
    s.maxHeight = '100%';
    s.bottom = '';
    s.right = '';
    s.zIndex = '';
    return s;
}

function applyMessagesStyleForWindow(messagesBase) {
    const s = { ...messagesBase };
    s.maxHeight = 'none';
    s.flex = '1';
    s.minHeight = '0';
    return s;
}

async function postJson(url, body) {
    const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body || {}),
    });
    const text = await res.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch (_) {
        data = text;
    }
    if (!res.ok) {
        const message =
            (data && (data.message || data.error)) ||
            `HTTP ${res.status} ${res.statusText}`;
        throw new Error(message);
    }
    return data;
}

async function getJson(url) {
    const res = await fetch(url);
    const text = await res.text();
    let data = null;
    try {
        data = text ? JSON.parse(text) : null;
    } catch (_) {
        data = text;
    }
    if (!res.ok) {
        const message =
            (data && (data.message || data.error)) ||
            `HTTP ${res.status} ${res.statusText}`;
        throw new Error(message);
    }
    return data;
}

async function mount() {
    const payload = await invoke('get_panel_payload');
    const ch = payload.chatPanel;
    const appCfg = payload.app || {};
    const root = document.getElementById('root');

    const executorBase = (appCfg.executorUrl || DEFAULT_EXECUTOR_URL).replace(/\/+$/, '');
    const pollInterval =
        Number.isFinite(appCfg.statusPollIntervalMs) && appCfg.statusPollIntervalMs > 0
            ? appCfg.statusPollIntervalMs
            : DEFAULT_POLL_INTERVAL_MS;

    const titleText =
        (appCfg.displayName && String(appCfg.displayName).trim()) || ch.strings.title;
    document.title = titleText;

    const panel = document.createElement('div');
    panel.id = ch.panelId;
    Object.assign(panel.style, applyPanelStyleForWindow(ch.panel));

    const header = document.createElement('div');
    Object.assign(header.style, {
        ...ch.header,
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
    });

    if (appCfg.headerIconDataUrl) {
        const img = document.createElement('img');
        img.src = appCfg.headerIconDataUrl;
        img.alt = '';
        Object.assign(img.style, {
            width: '24px',
            height: '24px',
            borderRadius: '6px',
            objectFit: 'cover',
            flexShrink: '0',
        });
        header.appendChild(img);
    }

    const titleEl = document.createElement('span');
    titleEl.textContent = titleText;
    titleEl.style.flex = '1';
    titleEl.style.minWidth = '0';
    header.appendChild(titleEl);

    const messages = document.createElement('div');
    messages.id = ch.messagesId;
    Object.assign(messages.style, applyMessagesStyleForWindow(ch.messages));

    const placeholder = document.createElement('div');
    Object.assign(placeholder.style, ch.placeholder);
    placeholder.textContent = ch.strings.placeholder;
    messages.appendChild(placeholder);

    const footer = document.createElement('div');
    Object.assign(footer.style, ch.footer);

    const input = document.createElement('input');
    input.type = 'text';
    input.placeholder = ch.strings.inputPlaceholder;
    input.setAttribute('aria-label', ch.strings.inputAriaLabel);
    Object.assign(input.style, ch.input);

    const btn = document.createElement('button');
    btn.type = 'button';
    btn.textContent = ch.strings.sendLabel;
    Object.assign(btn.style, ch.sendButton);

    function appendMessage(text, bubbleStyle) {
        if (placeholder.parentNode) {
            placeholder.remove();
        }
        const row = document.createElement('div');
        Object.assign(row.style, bubbleStyle);
        row.textContent = text;
        messages.appendChild(row);
        messages.scrollTop = messages.scrollHeight;
        return row;
    }

    function appendUserMessage(text) {
        return appendMessage(text, ch.userBubble);
    }

    function appendBotMessage(text) {
        return appendMessage(text, ch.botBubble || ch.userBubble);
    }

    function updateMessage(row, text) {
        row.textContent = text;
        messages.scrollTop = messages.scrollHeight;
    }

    async function pollUntilDone(runId, statusRow) {
        const terminal = new Set(['SUCCEEDED', 'FAILED']);
        while (true) {
            await new Promise(r => setTimeout(r, pollInterval));
            try {
                const status = await getJson(`${executorBase}/local/status/${runId}`);
                const summary = formatStatus(status);
                updateMessage(statusRow, summary);
                if (terminal.has(status.status)) {
                    return status;
                }
            } catch (err) {
                updateMessage(statusRow, `Не смогли получить статус: ${err.message}`);
                return null;
            }
        }
    }

    function formatStatus(status) {
        const totalSteps = status.totalSteps ?? 0;
        const failedSteps = status.failedSteps ?? 0;
        const msg = status.message ? ` — ${status.message}` : '';
        return `${status.status} • runId=${status.runId} • шагов: ${totalSteps} (упало: ${failedSteps})${msg}`;
    }

    async function send() {
        const text = input.value.trim();
        if (!text) {
            return;
        }
        appendUserMessage(text);
        input.value = '';
        btn.disabled = true;
        try {
            const initial = await postJson(`${executorBase}/local/run`, { userInput: text });
            const statusRow = appendBotMessage(formatStatus(initial));
            await pollUntilDone(initial.runId, statusRow);
        } catch (err) {
            appendBotMessage(`Ошибка запуска: ${err.message}`);
        } finally {
            btn.disabled = false;
        }
    }

    btn.addEventListener('click', send);
    input.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
            send();
        }
    });

    footer.appendChild(input);
    footer.appendChild(btn);
    panel.appendChild(header);
    panel.appendChild(messages);
    panel.appendChild(footer);
    root.appendChild(panel);
}

window.addEventListener('DOMContentLoaded', () => {
    mount().catch(err => {
        console.error(err);
        document.body.textContent = String(err.message || err);
    });
});
