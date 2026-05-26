package com.superpower.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    FAILED(400, "操作失败"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    VALIDATE_FAILED(422, "参数校验失败"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;
}
