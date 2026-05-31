package com.superpower.modules.requirement.dto;

import lombok.Data;

@Data
public class ReqItemDTO {
    private String title;
    private String description;
    private String priority;
    private String category;
    private String domain;
    private String type;
}
