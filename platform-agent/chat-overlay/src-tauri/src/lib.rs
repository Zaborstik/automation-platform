use std::fs;
use std::fs::OpenOptions;
use std::path::{Path, PathBuf};

use log::{info, warn};
use serde::Deserialize;
use serde_json::{json, Value};
use tauri::dpi::{PhysicalPosition, PhysicalSize};
use tauri::Manager;

#[derive(Debug, Deserialize)]
struct AppConfig {
    #[serde(rename = "displayName")]
    display_name: String,
    #[serde(default)]
    #[serde(rename = "iconPath")]
    icon_path: Option<String>,
    #[serde(default)]
    #[serde(rename = "logFilePath")]
    log_file_path: Option<String>,
    #[serde(default)]
    #[serde(rename = "windowMargin")]
    window_margin: Option<i32>,
    #[serde(default)]
    #[serde(rename = "windowHeightFraction")]
    window_height_fraction: Option<f64>,
    #[serde(default)]
    #[serde(rename = "executorUrl")]
    executor_url: Option<String>,
    #[serde(default)]
    #[serde(rename = "statusPollIntervalMs")]
    status_poll_interval_ms: Option<u64>,
}

fn dev_resources_dir() -> PathBuf {
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("../../src/main/resources")
}

fn resolve_resources_dir(app: &tauri::AppHandle) -> PathBuf {
    if let Ok(dir) = app.path().resource_dir() {
        if dir.join("chat-panel-ui.json").exists() {
            return dir;
        }
    }
    dev_resources_dir()
}

fn resolve_path_relative_to(base: &Path, p: &str) -> PathBuf {
    let path = Path::new(p);
    if path.is_absolute() {
        path.to_path_buf()
    } else {
        base.join(p)
    }
}

fn init_logging(resources: &Path) {
    let mut loggers: Vec<Box<dyn simplelog::SharedLogger>> = vec![simplelog::TermLogger::new(
        simplelog::LevelFilter::Info,
        simplelog::Config::default(),
        simplelog::TerminalMode::Mixed,
        simplelog::ColorChoice::Auto,
    )];

    if let Ok(raw) = fs::read_to_string(resources.join("chat-overlay-app.config.json")) {
        if let Ok(cfg) = serde_json::from_str::<AppConfig>(&raw) {
            if let Some(lp) = cfg
                .log_file_path
                .as_ref()
                .map(|s| s.trim())
                .filter(|s| !s.is_empty())
            {
                let p = resolve_path_relative_to(resources, lp);
                if let Some(parent) = p.parent() {
                    let _ = fs::create_dir_all(parent);
                }
                if let Ok(f) = OpenOptions::new().create(true).append(true).open(&p) {
                    loggers.push(simplelog::WriteLogger::new(
                        simplelog::LevelFilter::Info,
                        simplelog::Config::default(),
                        f,
                    ));
                }
            }
        }
    }

    let _ = simplelog::CombinedLogger::init(loggers);
}

fn load_app_config(resources: &Path) -> Result<AppConfig, String> {
    let raw = fs::read_to_string(resources.join("chat-overlay-app.config.json"))
        .map_err(|e| e.to_string())?;
    serde_json::from_str(&raw).map_err(|e| e.to_string())
}

fn panel_width_px(chat: &Value) -> i32 {
    chat.get("panel")
        .and_then(|p| p.get("width"))
        .and_then(|w| w.as_str())
        .and_then(|s| s.strip_suffix("px"))
        .and_then(|n| n.parse().ok())
        .unwrap_or(380)
}

#[tauri::command]
fn get_panel_payload(app: tauri::AppHandle) -> Result<Value, String> {
    let resources = resolve_resources_dir(&app);
    let panel_raw =
        fs::read_to_string(resources.join("chat-panel-ui.json")).map_err(|e| e.to_string())?;
    let chat_panel: Value = serde_json::from_str(&panel_raw).map_err(|e| e.to_string())?;
    let app_cfg = load_app_config(&resources)?;

    let mut app_json = json!({
        "displayName": app_cfg.display_name,
    });

    if let Some(ref url) = app_cfg.executor_url {
        app_json["executorUrl"] = json!(url);
    }
    if let Some(ms) = app_cfg.status_poll_interval_ms {
        app_json["statusPollIntervalMs"] = json!(ms);
    }

    if let Some(ref ip) = app_cfg.icon_path {
        let trimmed = ip.trim();
        if !trimmed.is_empty() {
            let full = resolve_path_relative_to(&resources, trimmed);
            if full.exists() {
                if let Ok(bytes) = fs::read(&full) {
                    let ext = full
                        .extension()
                        .and_then(|e| e.to_str())
                        .map(|e| e.to_lowercase())
                        .unwrap_or_default();
                    let mime = match ext.as_str() {
                        "png" => "image/png",
                        "jpg" | "jpeg" => "image/jpeg",
                        "gif" => "image/gif",
                        "webp" => "image/webp",
                        _ => "application/octet-stream",
                    };
                    use base64::Engine;
                    let b64 = base64::engine::general_purpose::STANDARD.encode(&bytes);
                    app_json["headerIconDataUrl"] =
                        json!(format!("data:{};base64,{}", mime, b64));
                }
            }
        }
    }

    Ok(json!({
        "chatPanel": chat_panel,
        "app": app_json,
    }))
}

fn apply_window_geometry(window: &tauri::WebviewWindow, resources: &Path) -> Result<(), String> {
    let app_cfg = load_app_config(resources)?;
    let panel_raw =
        fs::read_to_string(resources.join("chat-panel-ui.json")).map_err(|e| e.to_string())?;
    let chat_panel: Value = serde_json::from_str(&panel_raw).map_err(|e| e.to_string())?;
    let width = panel_width_px(&chat_panel) as u32;
    let margin = app_cfg.window_margin.unwrap_or(16);
    let frac = app_cfg
        .window_height_fraction
        .unwrap_or(0.45)
        .clamp(0.2, 0.95);

    let monitor = window
        .current_monitor()
        .map_err(|e| e.to_string())?
        .ok_or_else(|| "no monitor".to_string())?;

    let area = monitor.work_area();
    let height = ((area.size.height as f64) * frac).round() as u32;
    let x = area.position.x + area.size.width as i32 - width as i32 - margin;
    let y = area.position.y + area.size.height as i32 - height as i32 - margin;

    window
        .set_position(PhysicalPosition::new(x, y))
        .map_err(|e| e.to_string())?;
    window
        .set_size(PhysicalSize::new(width, height))
        .map_err(|e| e.to_string())?;

    window
        .set_title(&app_cfg.display_name)
        .map_err(|e| e.to_string())?;

    if let Some(ref ip) = app_cfg.icon_path {
        let trimmed = ip.trim();
        if !trimmed.is_empty() {
            let full = resolve_path_relative_to(resources, trimmed);
            if full.exists() {
                if let Ok(img) = tauri::image::Image::from_path(&full) {
                    let _ = window.set_icon(img.to_owned());
                }
            }
        }
    }

    Ok(())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .setup(|app| {
            let handle = app.handle().clone();
            let resources = resolve_resources_dir(&handle);
            init_logging(&resources);
            info!("chat-overlay resources: {:?}", resources);

            if let Some(window) = app.get_webview_window("main") {
                if let Err(e) = apply_window_geometry(&window, &resources) {
                    warn!("window geometry: {}", e);
                }
            }
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![get_panel_payload])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
