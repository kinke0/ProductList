package com.superpower.modules.requirement.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.repository.ImageResourceRepository;
import com.superpower.modules.requirement.dto.ReqActionDTO;
import com.superpower.modules.requirement.dto.ReqItemDTO;
import com.superpower.modules.requirement.entity.ReqItem;
import com.superpower.modules.requirement.entity.ReqLog;
import com.superpower.modules.requirement.repository.ReqItemRepository;
import com.superpower.modules.requirement.repository.ReqLogRepository;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RequirementService {

    private final ReqItemRepository reqItemRepository;
    private final ReqLogRepository reqLogRepository;
    private final SysUserRepository sysUserRepository;
    private final ImageResourceRepository imageResourceRepository;

    public RequirementService(ReqItemRepository reqItemRepository,
                              ReqLogRepository reqLogRepository,
                              SysUserRepository sysUserRepository,
                              ImageResourceRepository imageResourceRepository) {
        this.reqItemRepository = reqItemRepository;
        this.reqLogRepository = reqLogRepository;
        this.sysUserRepository = sysUserRepository;
        this.imageResourceRepository = imageResourceRepository;
    }

    public List<ReqItem> listAll(String status, Long createdBy, String startDate, String endDate,
                                 String category, String domain, String type, String priority) {
        List<ReqItem> result;
        if (status == null && createdBy == null && startDate == null && endDate == null
                && category == null && domain == null && type == null && priority == null) {
            result = reqItemRepository.findAllByOrderByCreatedAtDesc();
        } else {
            LocalDateTime startDt = null;
            LocalDateTime endDt = null;
            if (startDate != null && !startDate.isBlank()) {
                startDt = LocalDate.parse(startDate).atStartOfDay();
            }
            if (endDate != null && !endDate.isBlank()) {
                endDt = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            }
            result = reqItemRepository.findByFilters(status, createdBy, startDt, endDt, category, domain, type, priority);
        }
        fillCreatorNames(result);
        return result;
    }

    public List<ReqItem> listByUser(Long userId) {
        List<ReqItem> result = reqItemRepository.findByCreatedByOrderByCreatedAtDesc(userId);
        fillCreatorNames(result);
        return result;
    }

    private void fillCreatorNames(List<ReqItem> items) {
        Set<Long> userIds = items.stream().map(ReqItem::getCreatedBy).filter(id -> id != null).collect(Collectors.toSet());
        Map<Long, String> nameMap = new HashMap<>();
        for (Long uid : userIds) {
            sysUserRepository.findById(uid).ifPresent(u -> nameMap.put(uid, u.getNickname() != null ? u.getNickname() : u.getUsername()));
        }
        items.forEach(item -> item.setCreatorName(nameMap.getOrDefault(item.getCreatedBy(), "未知")));
    }

    public ReqItem getById(Long id) {
        return reqItemRepository.findById(id)
                .orElseThrow(() -> new BusinessException("需求不存在"));
    }

    @Transactional
    public ReqItem create(Long userId, ReqItemDTO dto) {
        ReqItem item = new ReqItem();
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPriority(dto.getPriority() != null ? dto.getPriority() : "中");
        item.setCategory(dto.getCategory());
        item.setDomain(dto.getDomain());
        item.setType(dto.getType());
        item.setCreatedBy(userId);
        item.setStatus("提出");
        item.setReqNo(generateReqNo());
        ReqItem saved = reqItemRepository.save(item);
        addLog(saved.getId(), "创建", null, userId);
        return saved;
    }

    @Transactional
    public ReqItem update(Long id, Long userId, ReqItemDTO dto) {
        ReqItem item = getById(id);
        if (!item.getCreatedBy().equals(userId)) {
            throw new BusinessException("只能编辑自己提出的需求");
        }
        if (!"提出".equals(item.getStatus())) {
            throw new BusinessException("只有\"提出\"状态的需求可以编辑");
        }
        item.setTitle(dto.getTitle());
        item.setDescription(dto.getDescription());
        item.setPriority(dto.getPriority());
        item.setCategory(dto.getCategory());
        item.setDomain(dto.getDomain());
        item.setType(dto.getType());
        item.setUpdatedAt(LocalDateTime.now());
        return reqItemRepository.save(item);
    }

    @Transactional
    public ReqItem confirm(Long id, Long operatorId, ReqActionDTO dto) {
        ReqItem item = getById(id);
        ensureStatus(item, "提出");
        item.setStatus("已确认");
        item.setUpdatedAt(LocalDateTime.now());
        addLog(id, "已确认", dto != null ? dto.getComment() : null, operatorId);
        return reqItemRepository.save(item);
    }

    @Transactional
    public ReqItem develop(Long id, Long operatorId, ReqActionDTO dto) {
        ReqItem item = getById(id);
        ensureStatus(item, "已确认");
        item.setStatus("开发中");
        item.setUpdatedAt(LocalDateTime.now());
        addLog(id, "开发中", dto != null ? dto.getComment() : null, operatorId);
        return reqItemRepository.save(item);
    }

    @Transactional
    public ReqItem ready(Long id, Long operatorId, ReqActionDTO dto) {
        ReqItem item = getById(id);
        ensureStatus(item, "开发中");
        item.setStatus("待上线");
        item.setUpdatedAt(LocalDateTime.now());
        addLog(id, "待上线", dto != null ? dto.getComment() : null, operatorId);
        return reqItemRepository.save(item);
    }

    @Transactional
    public ReqItem release(Long id, Long operatorId, ReqActionDTO dto) {
        ReqItem item = getById(id);
        ensureStatus(item, "待上线");
        if (dto == null || dto.getReleasedVersion() == null || dto.getReleasedVersion().isBlank()) {
            throw new BusinessException("上线操作必须填写版本号");
        }
        item.setStatus("已上线");
        item.setReleasedVersion(dto.getReleasedVersion());
        item.setUpdatedAt(LocalDateTime.now());
        addLog(id, "已上线", "版本: " + dto.getReleasedVersion(), operatorId);
        return reqItemRepository.save(item);
    }

    @Transactional
    public ReqItem reject(Long id, Long operatorId, ReqActionDTO dto) {
        ReqItem item = getById(id);
        if (!"提出".equals(item.getStatus()) && !"已确认".equals(item.getStatus())) {
            throw new BusinessException("当前状态不允许驳回");
        }
        if (dto == null || dto.getRejectReason() == null || dto.getRejectReason().isBlank()) {
            throw new BusinessException("驳回必须填写原因");
        }
        item.setStatus("驳回");
        item.setRejectReason(dto.getRejectReason());
        item.setUpdatedAt(LocalDateTime.now());
        addLog(id, "驳回", dto.getRejectReason(), operatorId);
        return reqItemRepository.save(item);
    }

    @Transactional
    public ReqItem cancel(Long id, Long userId) {
        ReqItem item = getById(id);
        if (!item.getCreatedBy().equals(userId)) {
            throw new BusinessException("只能撤销自己提出的需求");
        }
        if (!"提出".equals(item.getStatus())) {
            throw new BusinessException("只有\"提出\"状态的需求可以撤销");
        }
        item.setStatus("撤销");
        item.setUpdatedAt(LocalDateTime.now());
        addLog(id, "撤销", "提出人主动撤销", userId);
        return reqItemRepository.save(item);
    }

    @Transactional
    public void delete(Long id) {
        ReqItem item = getById(id);
        if (item.getDescription() != null) {
            Matcher m = Pattern.compile("data-id=\"(\\d+)\"").matcher(item.getDescription());
            while (m.find()) {
                Long imgId = Long.parseLong(m.group(1));
                imageResourceRepository.findById(imgId).ifPresent(img -> {
                    List<ReqItem> all = reqItemRepository.findAll();
                    long refCount = all.stream()
                            .filter(r -> !r.getId().equals(id) && r.getDescription() != null
                                    && r.getDescription().contains(img.getUrl()))
                            .count();
                    if (refCount == 0) {
                        try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(img.getPath())); } catch (java.io.IOException ignored) {}
                        imageResourceRepository.deleteById(imgId);
                    }
                });
            }
        }
        reqLogRepository.deleteAll(reqLogRepository.findByReqIdOrderByCreatedAtDesc(id));
        reqItemRepository.deleteById(id);
    }

    public List<ReqLog> getLogs(Long reqId) {
        List<ReqLog> logs = reqLogRepository.findByReqIdOrderByCreatedAtDesc(reqId);
        logs.forEach(log -> log.setOperatorName(getNickname(log.getOperatedBy())));
        return logs;
    }

    public Map<String, Long> getStatusStats(String status, Long createdBy, String startDate, String endDate,
                                            String category, String domain, String type, String priority) {
        List<ReqItem> list = listAll(status, createdBy, startDate, endDate, category, domain, type, priority);
        return list.stream().collect(Collectors.groupingBy(ReqItem::getStatus, Collectors.counting()));
    }

    public Map<String, Long> getModuleStats(String status, Long createdBy, String startDate, String endDate,
                                            String category, String domain, String type, String priority) {
        List<ReqItem> list = listAll(status, createdBy, startDate, endDate, category, domain, type, priority);
        return list.stream().collect(Collectors.groupingBy(
                item -> item.getCategory() != null ? item.getCategory() : "未分类",
                Collectors.counting()));
    }

    public Map<String, Long> getTypeStats(String status, Long createdBy, String startDate, String endDate,
                                          String category, String domain, String type, String priority) {
        List<ReqItem> list = listAll(status, createdBy, startDate, endDate, category, domain, type, priority);
        return list.stream().collect(Collectors.groupingBy(
                item -> item.getType() != null ? item.getType() : "未分类",
                Collectors.counting()));
    }

    public String getNickname(Long userId) {
        return sysUserRepository.findById(userId)
                .map(u -> u.getNickname() != null ? u.getNickname() : u.getUsername())
                .orElse("未知");
    }

    private void ensureStatus(ReqItem item, String expected) {
        if (!expected.equals(item.getStatus())) {
            throw new BusinessException("需求状态不是\"" + expected + "\"，无法执行此操作");
        }
    }

    private String generateReqNo() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        String prefix = "REQ-" + year + "-";
        long count = reqItemRepository.countByReqNoPrefix(prefix + "%");
        return prefix + String.format("%04d", count + 1);
    }

    private void addLog(Long reqId, String action, String comment, Long operatorId) {
        ReqLog log = new ReqLog();
        log.setReqId(reqId);
        log.setAction(action);
        log.setComment(comment);
        log.setOperatedBy(operatorId);
        reqLogRepository.save(log);
    }
}
