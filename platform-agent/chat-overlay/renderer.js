if (!window.__TAURI__?.core) {
    document.body.textContent =
        'Ожидается Tauri (window.__TAURI__). Запускайте через platform-chat-overlay, не открывайте index.html в браузере.';
    throw new Error('Tauri API missing');
}
const { invoke } = window.__TAURI__.core;

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

async function mount() {
    const payload = await invoke('get_panel_payload');
    const ch = payload.chatPanel;
    const appCfg = payload.app || {};
    const root = document.getElementById('root');

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

    function appendUserMessage(text) {
        if (placeholder.parentNode) {
            placeholder.remove();
        }
        const row = document.createElement('div');
        Object.assign(row.style, ch.userBubble);
        row.textContent = text;
        messages.appendChild(row);
        messages.scrollTop = messages.scrollHeight;
    }

    function send() {
        const text = input.value.trim();
        if (!text) {
            return;
        }
        appendUserMessage(text);
        input.value = '';
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
