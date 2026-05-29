package com.superpower.modules.data.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ExcelImportResult {
    private int totalRows;
    private int successRows;
    private int updateRows;
    private int failRows;
    private List<String> errors = new ArrayList<>();
}
