package com.superpower.modules.system.dto;

import lombok.Data;
import java.util.List;

@Data
public class SqlExecutionResult {
    private int index;
    private String statement;
    private boolean success;
    private String message;
    private int affectedRows;
    private long durationMs;
    private List<String> columns;
    private List<List<Object>> rows;

    public static SqlExecutionResult success(int index, String statement, int affectedRows, long durationMs) {
        SqlExecutionResult r = new SqlExecutionResult();
        r.index = index;
        r.statement = statement;
        r.success = true;
        r.affectedRows = affectedRows;
        r.durationMs = durationMs;
        r.message = "执行成功" + (affectedRows >= 0 ? "，影响行数: " + affectedRows : "");
        return r;
    }

    public static SqlExecutionResult querySuccess(int index, String statement, int rowCount, List<String> columns, List<List<Object>> rows, long durationMs) {
        SqlExecutionResult r = new SqlExecutionResult();
        r.index = index;
        r.statement = statement;
        r.success = true;
        r.affectedRows = rowCount;
        r.columns = columns;
        r.rows = rows;
        r.durationMs = durationMs;
        r.message = "查询成功，返回 " + rowCount + " 行";
        return r;
    }

    public static SqlExecutionResult failure(int index, String statement, String errorMessage, long durationMs) {
        SqlExecutionResult r = new SqlExecutionResult();
        r.index = index;
        r.statement = statement;
        r.success = false;
        r.affectedRows = 0;
        r.durationMs = durationMs;
        r.message = errorMessage;
        return r;
    }
}
