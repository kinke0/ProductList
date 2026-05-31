package com.superpower.modules.approval.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.approval.entity.ApprovalLog;
import com.superpower.modules.approval.repository.ApprovalLogRepository;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.version.service.VersionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ApprovalService {

    private static final String ST_PENDING = "待提交";
    private static final String ST_REVIEW = "待审核";
    private static final String ST_APPROVED = "审核通过";
    private static final String ST_REJECTED = "驳回";

    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
        ST_PENDING, Set.of(ST_REVIEW),
        ST_REVIEW, Set.of(ST_APPROVED, ST_REJECTED, ST_PENDING),
        ST_APPROVED, Set.of(ST_REJECTED),
        ST_REJECTED, Set.of(ST_REVIEW)
    );

    private static final Map<String, Set<String>> ACTION_ROLES = Map.of(
        "submit", Set.of("editor", "admin"),
        "approve", Set.of("admin"),
        "reject", Set.of("admin"),
        "withdraw", Set.of("editor", "admin")
    );

    private static final Map<String, String> ACTION_TARGET = Map.of(
        "submit", ST_REVIEW,
        "approve", ST_APPROVED,
        "reject", ST_REJECTED,
        "withdraw", ST_PENDING
    );

    private static final Map<String, String> ROLE_CODE_MAP = Map.of(
        "ADMIN", "admin",
        "USER", "editor"
    );

    private final DataEntryRepository entryRepository;
    private final ApprovalLogRepository logRepository;
    private final VersionService versionService;

    public ApprovalService(DataEntryRepository entryRepository,
                           ApprovalLogRepository logRepository,
                           VersionService versionService) {
        this.entryRepository = entryRepository;
        this.logRepository = logRepository;
        this.versionService = versionService;
    }

    @Transactional
    public void approve(Long entryId, String action, String sysRoleCode, Long userId, String userName, String comment) {
        String role = ROLE_CODE_MAP.getOrDefault(sysRoleCode, "editor");

        if (!ACTION_ROLES.getOrDefault(action, Set.of()).contains(role)) {
            throw new BusinessException("无权执行此操作");
        }

        if ("withdraw".equals(action) && !"admin".equals(role)) {
            ApprovalLog lastSubmit = logRepository.findTopByEntryIdAndActionOrderByCreatedAtDesc(entryId, "submit").orElse(null);
            if (lastSubmit == null || !userId.equals(lastSubmit.getOperatorId())) {
                throw new BusinessException("仅提交人可撤销");
            }
        }

        DataEntry entry = entryRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException("数据不存在"));

        if (!versionService.isEditable(entry.getVersionId())) {
            throw new BusinessException("仅编辑中版本可变更审批状态");
        }

        String currentStatus = entry.getApprovalStatus();
        if (currentStatus == null || currentStatus.isEmpty()) {
            currentStatus = ST_PENDING;
        }

        String targetStatus = ACTION_TARGET.get(action);
        Set<String> allowed = TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessException("当前状态不允许此操作：" + currentStatus + " → " + targetStatus);
        }

        entry.setApprovalStatus(targetStatus);
        entryRepository.save(entry);

        ApprovalLog log = new ApprovalLog();
        log.setEntryId(entryId);
        log.setFromStatus(currentStatus);
        log.setToStatus(targetStatus);
        log.setAction(action);
        log.setOperatorId(userId);
        log.setOperatorName(userName);
        log.setComment(comment);
        logRepository.save(log);
    }

    public List<ApprovalLog> getLogs(Long entryId) {
        return logRepository.findByEntryIdOrderByCreatedAtDesc(entryId);
    }

    public static boolean canEdit(String approvalStatus, String sysRoleCode) {
        String role = ROLE_CODE_MAP.getOrDefault(sysRoleCode, "editor");
        if ("admin".equals(role)) return true;
        if ("editor".equals(role)) return approvalStatus == null || ST_PENDING.equals(approvalStatus) || ST_REJECTED.equals(approvalStatus);
        return false;
    }
}
