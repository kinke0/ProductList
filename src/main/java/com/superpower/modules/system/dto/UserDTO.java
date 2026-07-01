package com.superpower.modules.system.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String nickname;
    private Long roleId;
    private String roleName;
    private String roleCode;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private Boolean online;
}
