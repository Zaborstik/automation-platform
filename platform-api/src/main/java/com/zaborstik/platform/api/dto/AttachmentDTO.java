package com.zaborstik.platform.api.dto;

import java.util.Map;

/**
 * DTO для вложений. Та же единая модель (tableName + id + data), tableName = "attachments".
 */
public class AttachmentDTO extends EntityDTO {

    public AttachmentDTO() {
        setTableName(EntityDTO.TABLE_ATTACHMENTS);
    }

    public AttachmentDTO(String id, Map<String, Object> data) {
        super(EntityDTO.TABLE_ATTACHMENTS, id, data);
    }

    public AttachmentDTO(EntityDTO dto) {
        setTableName(EntityDTO.TABLE_ATTACHMENTS);
        setId(dto.getId());
        setData(dto.getData());
    }

    public String getParentTable() {
        return get("parentTable");
    }

    public String getParentId() {
        return get("parentId");
    }

    public String getFilename() {
        return get("filename");
    }

    public String getContentType() {
        return get("contentType");
    }

    public String getFilePath() {
        return get("filePath");
    }
}
