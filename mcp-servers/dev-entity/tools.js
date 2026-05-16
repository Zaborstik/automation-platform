import {
  encodeQueryValue,
  getWithAuth,
  postWithAuth,
  requireValue,
} from "./auth.js";

const ENTITY_PATH = "/json/v2/mcp/entities";

const FIELD_TYPES = new Set([
  "INTEGER",
  "DOUBLE",
  "BOOLEAN",
  "STRING",
  "DATETIME",
  "DATE",
  "TIME",
  "TEXT",
  "BIGINT",
]);

export const toolDefinitions = [
  {
    name: "create_table",
    description: "Create an entity type (table).",
    inputSchema: {
      type: "object",
      properties: {
        tableName: {
          type: "string",
          description: "Table name (required).",
        },
        displayName: {
          type: "string",
          description: "Display name (optional). Defaults to tableName.",
        },
      },
      required: ["tableName"],
    },
  },
  {
    name: "add_field",
    description:
      "Add a field to an entity type. Use fieldType or specify lookupEntity/lookupField for lookups.",
    inputSchema: {
      type: "object",
      properties: {
        tableName: {
          type: "string",
          description: "Target table name (required).",
        },
        fieldName: {
          type: "string",
          description: "Field name (required).",
        },
        fieldType: {
          type: "string",
          description:
            "Optional data type. One of: " +
            Array.from(FIELD_TYPES).join(", "),
        },
        displayName: {
          type: "string",
          description:
            "Display name (optional). Defaults to camelCaseToCaption(fieldName).",
        },
        lookupEntity: {
          type: "string",
          description: "Lookup entity name for AUTOCOMPLETE fields (optional).",
        },
        lookupField: {
          type: "string",
          description:
            "Lookup field name (optional). Defaults to lookup entity PK.",
        },
      },
      required: ["tableName", "fieldName"],
    },
  },
  {
    name: "list_tables",
    description: "List all entity types (tables).",
    inputSchema: {
      type: "object",
      properties: {},
    },
  },
  {
    name: "get_table",
    description: "Fetch an entity type by id or table name.",
    inputSchema: {
      type: "object",
      properties: {
        idOrName: {
          type: "string",
          description: "Table id or table name.",
        },
        tableName: {
          type: "string",
          description: "Table name.",
        },
      },
      additionalProperties: false,
      required: [],
    },
  },
  {
    name: "create_rows",
    description: "Create one or more rows in an entity type.",
    inputSchema: {
      type: "object",
      properties: {
        tableName: {
          type: "string",
          description: "Target table name (required).",
        },
        item: {
          type: "object",
          description: "Single row to insert (optional).",
        },
        items: {
          type: "array",
          items: { type: "object" },
          description: "Rows to insert in bulk (optional).",
        },
        returnFields: {
          type: "array",
          items: { type: "string" },
          description: "Optional list of fields to return.",
        },
      },
      required: ["tableName"],
    },
  },
  {
    name: "read_rows",
    description: "Read rows from an entity type with optional filtering.",
    inputSchema: {
      type: "object",
      properties: {
        tableName: {
          type: "string",
          description: "Target table name (required).",
        },
        whereQuery: {
          type: "string",
          description: "Optional where query string.",
        },
        params: {
          type: "object",
          description: "Optional params map for whereQuery.",
        },
        filters: {
          type: "object",
          description: "Optional filters JSON.",
        },
        offset: {
          type: "integer",
          description: "Optional offset (default 0).",
        },
        limit: {
          type: "integer",
          description: "Optional limit (default 50, max 500).",
        },
        orderBy: {
          type: "string",
          description:
            "Optional orderBy string. You can pass JSON array as a string, for example: " +
            "[{\"field\":\"name\",\"direction\":\"asc\"}]",
        },
        includeTotal: {
          type: "boolean",
          description: "Optional includeTotal flag.",
        },
      },
      required: ["tableName"],
    },
  },
  {
    name: "update_rows",
    description: "Update one or more rows in an entity type.",
    inputSchema: {
      type: "object",
      properties: {
        tableName: {
          type: "string",
          description: "Target table name (required).",
        },
        item: {
          type: "object",
          description: "Single row update (optional). Must include PK.",
        },
        items: {
          type: "array",
          items: { type: "object" },
          description: "Row updates in bulk (optional). Must include PK.",
        },
        returnFields: {
          type: "array",
          items: { type: "string" },
          description: "Optional list of fields to return.",
        },
      },
      required: ["tableName"],
    },
  },
  {
    name: "delete_rows",
    description: "Delete one or more rows in an entity type.",
    inputSchema: {
      type: "object",
      properties: {
        tableName: {
          type: "string",
          description: "Target table name (required).",
        },
        item: {
          type: "object",
          description: "Single row delete (optional). Must include PK.",
        },
        items: {
          type: "array",
          items: { type: "object" },
          description: "Row deletes in bulk (optional). Must include PK.",
        },
      },
      required: ["tableName"],
    },
  },
];

function textContent(data) {
  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(data?.contents ?? data, null, 2),
      },
    ],
  };
}

async function createTable(args) {
  requireValue(args?.tableName, "tableName");
  const payload = {
    tableName: args.tableName,
  };

  if (args?.displayName) {
    payload.displayName = args.displayName;
  }

  const data = await postWithAuth(ENTITY_PATH, payload);
  return textContent(data);
}

async function addField(args) {
  requireValue(args?.tableName, "tableName");
  requireValue(args?.fieldName, "fieldName");

  if (args?.fieldType && !FIELD_TYPES.has(args.fieldType)) {
    throw new Error(
      `fieldType must be one of: ${Array.from(FIELD_TYPES).join(", ")}`
    );
  }

  if (args?.lookupField && !args?.lookupEntity) {
    throw new Error("lookupField requires lookupEntity.");
  }

  const payload = {
    fieldName: args.fieldName,
  };

  if (args?.fieldType) {
    payload.fieldType = args.fieldType;
  }

  if (args?.displayName) {
    payload.displayName = args.displayName;
  }

  if (args?.lookupEntity) {
    payload.lookupEntity = args.lookupEntity;
  }

  if (args?.lookupField) {
    payload.lookupField = args.lookupField;
  }

  const data = await postWithAuth(
    `${ENTITY_PATH}/${encodeURIComponent(args.tableName)}/fields`,
    payload
  );

  return textContent(data);
}

async function listTables() {
  const data = await getWithAuth(ENTITY_PATH);

  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(
          data?.contents?.items ?? data?.contents ?? data,
          null,
          2
        ),
      },
    ],
  };
}

async function getTable(args) {
  const idOrName = args?.idOrName ?? args?.tableName;
  requireValue(idOrName, "idOrName");

  const data = await getWithAuth(
    `${ENTITY_PATH}/${encodeURIComponent(idOrName)}`
  );
  return textContent(data);
}

function buildRowsPayload(args, { allowReturnFields = true } = {}) {
  if (args?.item && typeof args.item !== "object") {
    throw new Error("item must be an object when provided.");
  }

  if (args?.items && !Array.isArray(args.items)) {
    throw new Error("items must be an array when provided.");
  }

  if (!args?.item && !args?.items) {
    throw new Error("Either item or items must be provided.");
  }

  const payload = {};
  if (args?.item) {
    payload.item = args.item;
  }
  if (args?.items) {
    payload.items = args.items;
  }
  if (allowReturnFields && args?.returnFields) {
    payload.returnFields = args.returnFields;
  }
  return payload;
}

async function createRows(args) {
  requireValue(args?.tableName, "tableName");
  const payload = buildRowsPayload(args);

  const data = await postWithAuth(
    `${ENTITY_PATH}/${encodeURIComponent(args.tableName)}/rows`,
    payload
  );
  return textContent(data);
}

async function readRows(args) {
  requireValue(args?.tableName, "tableName");

  const queryParams = {
    whereQuery: encodeQueryValue(args?.whereQuery),
    params: encodeQueryValue(args?.params),
    filters: encodeQueryValue(args?.filters),
    offset:
      typeof args?.offset === "number" ? String(args.offset) : undefined,
    limit: typeof args?.limit === "number" ? String(args.limit) : undefined,
    orderBy: encodeQueryValue(args?.orderBy),
    includeTotal:
      typeof args?.includeTotal === "boolean"
        ? String(args.includeTotal)
        : undefined,
  };

  const data = await getWithAuth(
    `${ENTITY_PATH}/${encodeURIComponent(args.tableName)}/rows`,
    queryParams
  );
  return textContent(data);
}

async function updateRows(args) {
  requireValue(args?.tableName, "tableName");
  const payload = buildRowsPayload(args);

  const data = await postWithAuth(
    `${ENTITY_PATH}/${encodeURIComponent(args.tableName)}/rows/update`,
    payload
  );
  return textContent(data);
}

async function deleteRows(args) {
  requireValue(args?.tableName, "tableName");
  const payload = buildRowsPayload(args, { allowReturnFields: false });

  const data = await postWithAuth(
    `${ENTITY_PATH}/${encodeURIComponent(args.tableName)}/rows/delete`,
    payload
  );
  return textContent(data);
}

const handlers = {
  create_table: createTable,
  add_field: addField,
  list_tables: listTables,
  get_table: getTable,
  create_rows: createRows,
  read_rows: readRows,
  update_rows: updateRows,
  delete_rows: deleteRows,
};

export async function callTool(name, args) {
  const handler = handlers[name];
  if (!handler) {
    throw new Error(`Unknown tool: ${name}`);
  }
  return handler(args);
}
