package com.superpower.modules.customtab.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomTabEntryId implements Serializable {
    private Long customTabId;
    private Long entryId;
}
