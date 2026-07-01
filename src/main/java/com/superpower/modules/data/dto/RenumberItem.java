package com.superpower.modules.data.dto;

import lombok.Data;

@Data
public class RenumberItem {
    private Long entryId;
    private String newPrefix;
    private String newName;
}
