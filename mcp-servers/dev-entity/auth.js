const BASE_URL = process.env.MCP_ENTITY_BASE_URL || "http://localhost:8080";
const USER = process.env.MCP_ENTITY_USER || "";
const PASSWORD = process.env.MCP_ENTITY_PASSWORD || "";

const LOGIN_PATH = "/json/login";

let cachedToken = null;

export function buildUrl(pathname, queryParams) {
  const url = new URL(BASE_URL);
  url.pathname = pathname;
  if (queryParams) {
    Object.entries(queryParams).forEach(([key, value]) => {
      if (value === undefined || value === null) {
        return;
      }
      url.searchParams.set(key, value);
    });
  }
  return url.toString();
}

export function requireValue(value, label) {
  if (typeof value !== "string" || value.trim() === "") {
    throw new Error(`${label} is required and must be a non-empty string.`);
  }
}

export function parseJsonResponse(text) {
  if (!text) {
    return null;
  }

  try {
    return JSON.parse(text);
  } catch (error) {
    const snippet = text.slice(0, 200);
    const err = new Error(`Invalid JSON response: ${snippet}`);
    err.rawText = text;
    throw err;
  }
}

export function summarizeError(data) {
  if (!data) {
    return "empty response";
  }
  if (typeof data === "string") {
    return data;
  }
  if (data.message) {
    return data.message;
  }
  return JSON.stringify(data);
}

export function encodeQueryValue(value) {
  if (value === undefined || value === null) {
    return undefined;
  }
  if (typeof value === "string") {
    return value;
  }
  return JSON.stringify(value);
}

async function login() {
  if (!USER || !PASSWORD) {
    throw new Error(
      "MCP_ENTITY_USER and MCP_ENTITY_PASSWORD must be set for authentication."
    );
  }

  const response = await fetch(buildUrl(LOGIN_PATH), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ user: USER, password: PASSWORD }),
  });

  const text = await response.text();
  const data = parseJsonResponse(text);

  if (!response.ok) {
    throw new Error(
      `Login failed (${response.status}): ${summarizeError(data)}`
    );
  }

  const token = data?.token?.value;
  if (!token) {
    throw new Error("Login response missing token.value.");
  }

  cachedToken = token;
  return token;
}

async function getToken() {
  if (cachedToken) {
    return cachedToken;
  }
  return login();
}

async function requestWithAuth(method, pathname, body, queryParams) {
  const maxAttempts = 2;
  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    const token = await getToken();
    const response = await fetch(buildUrl(pathname, queryParams), {
      method,
      headers: {
        "Content-Type": "application/json",
        "X-Auth": token,
      },
      body: body ? JSON.stringify(body) : undefined,
    });

    const text = await response.text();
    let data = null;
    try {
      data = parseJsonResponse(text);
    } catch (error) {
      const illegalToken = text?.toLowerCase().includes("illegal token");
      if (illegalToken) {
        cachedToken = null;
        if (attempt < maxAttempts) {
          continue;
        }
      }
      throw error;
    }

    if (response.status === 401 || response.status === 403) {
      cachedToken = null;
      if (attempt < maxAttempts) {
        continue;
      }
    }

    if (!response.ok) {
      const message =
        text && !data ? text : summarizeError(data);
      throw new Error(`Request failed (${response.status}): ${message}`);
    }

    return data;
  }

  throw new Error("Request failed after retrying authentication.");
}

export async function postWithAuth(pathname, body) {
  return requestWithAuth("POST", pathname, body);
}

export async function getWithAuth(pathname, queryParams) {
  return requestWithAuth("GET", pathname, null, queryParams);
}
