package com.superpower.modules.requirement.dto;

import lombok.Data;

@Data
public class ReqActionDTO {
    private String comment;
    private String rejectReason;
    private String releasedVersion;
}
