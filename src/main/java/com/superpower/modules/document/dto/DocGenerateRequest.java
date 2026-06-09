package com.superpower.modules.document.dto;

import lombok.Data;
import java.util.List;

@Data
public class DocGenerateRequest {
    private Long versionId;
    private String docName;
    private String docType;
    private String format;
    private String dataScope;
    private List<Long> entryIds;
    private Long customTabId;
    private Boolean includeImages = true;
}
