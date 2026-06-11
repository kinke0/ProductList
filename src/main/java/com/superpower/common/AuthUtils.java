package com.superpower.common;

import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.service.SysUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

public final class AuthUtils {

    private AuthUtils() {}

    public static Long getUserId(Authentication auth, SysUserService userService) {
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof UserDetails ud) {
            SysUser user = userService.findByUsername(ud.getUsername());
            return user != null ? user.getId() : null;
        }
        return null;
    }

    public static String getUsername(Authentication auth) {
        if (auth == null) return "system";
        if (auth.getPrincipal() instanceof UserDetails ud) {
            return ud.getUsername();
        }
        return "system";
    }
}
