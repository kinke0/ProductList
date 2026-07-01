package com.superpower.modules.system.service;

import com.superpower.modules.system.entity.OperationLog;
import com.superpower.modules.system.repository.OperationLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
public class OperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(OperationLogService.class);

    private final OperationLogRepository repository;

    public OperationLogService(OperationLogRepository repository) {
        this.repository = repository;
    }

    public void record(Long userId, String username, String action, String module, String description) {
        record(userId, username, action, module, description, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String username, String action, String module, String description,
                       Long targetId, String targetType) {
        try {
            OperationLog log = new OperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setAction(action);
            log.setModule(module);
            log.setDescription(description);
            log.setTargetId(targetId);
            log.setTargetType(targetType);
            log.setIp(getClientIp());
            repository.save(log);
        } catch (Exception e) {
            logger.warn("操作日志写入失败: userId={}, action={}, desc={}, error={}", userId, action, description, e.getMessage());
        }
    }

    public List<OperationLog> getByUserId(Long userId) {
        return repository.findRecentByUserId(userId, 200);
    }

    public List<OperationLog> getAll() {
        return repository.findRecent(500);
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("Proxy-Client-IP");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("WL-Proxy-Client-IP");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
            if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return "127.0.0.1";
            if (ip != null && ip.startsWith("::ffff:")) return ip.substring(7);
            return ip != null ? ip : "";
        } catch (Exception e) {
            return "";
        }
    }
}
