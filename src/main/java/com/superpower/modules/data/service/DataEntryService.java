package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.approval.entity.ApprovalLog;
import com.superpower.modules.approval.repository.ApprovalLogRepository;
import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.ExcelImportResult;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.system.entity.SysUser;
import com.superpower.modules.system.repository.SysUserRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DataEntryService {

    private final DataEntryRepository entryRepository;
    private final DataVersionRepository dataVersionRepository;
    private final CustomTabEntryRepository customTabEntryRepository;
    private final ApprovalLogRepository approvalLogRepository;
    private final SysUserRepository sysUserRepository;
    private final BaseCategoryRepository baseCategoryRepository;
    private final BaseDomainRepository baseDomainRepository;

    public DataEntryService(DataEntryRepository entryRepository, DataVersionRepository dataVersionRepository,
                            CustomTabEntryRepository customTabEntryRepository,
                            ApprovalLogRepository approvalLogRepository,
                            SysUserRepository sysUserRepository,
                            BaseCategoryRepository baseCategoryRepository,
                            BaseDomainRepository baseDomainRepository) {
        this.entryRepository = entryRepository;
        this.dataVersionRepository = dataVersionRepository;
        this.customTabEntryRepository = customTabEntryRepository;
        this.approvalLogRepository = approvalLogRepository;
        this.sysUserRepository = sysUserRepository;
        this.baseCategoryRepository = baseCategoryRepository;
        this.baseDomainRepository = baseDomainRepository;
    }

    private boolean matchesStatus(String colStatus, List<String> statusList) {
        if (statusList == null || statusList.isEmpty()) return true;
        if (colStatus == null || colStatus.isEmpty()) return false;
        for (String s : statusList) {
            if (colStatus.contains(s)) return true;
        }
        return false;
    }

    public List<TreeNodeDTO> getTree(Long versionId, String name, List<String> statusList, String productManager,
                                     String solution, String versionTag) {
        List<DataEntry> entries = entryRepository.findByVersionIdAndLevelWithFilter(
                versionId, 1, name, productManager, solution, versionTag);

        Map<Long, BaseCategory> catMap = new HashMap<>();
        Map<Long, BaseDomain> domMap = new HashMap<>();
        baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId)
            .forEach(c -> catMap.put(c.getId(), c));
        baseDomainRepository.findByVersionId(versionId)
            .forEach(d -> domMap.put(d.getId(), d));

        return entries.stream()
                .filter(e -> matchesStatus(e.getColStatus(), statusList))
                .map(e -> buildTree(e, versionId, catMap, domMap)).toList();
    }

    private TreeNodeDTO buildTree(DataEntry entry, Long versionId, Map<Long, BaseCategory> catMap, Map<Long, BaseDomain> domMap) {
        TreeNodeDTO node = new TreeNodeDTO();
        node.setId(entry.getId());
        node.setParentId(entry.getParentId());
        node.setLevel(entry.getLevel());

        String label;
        if (entry.getLevel() == 1 && entry.getCategoryId() != null && catMap.containsKey(entry.getCategoryId())) {
            label = catMap.get(entry.getCategoryId()).getName();
        } else if (entry.getLevel() == 2 && entry.getDomainId() != null && domMap.containsKey(entry.getDomainId())) {
            label = domMap.get(entry.getDomainId()).getName();
        } else {
            label = entry.getColProductSystem() != null ? entry.getColProductSystem() : entry.getColBizCategory();
        }
        node.setLabel(label);
        node.setSortOrder(entry.getSortOrder());

        if (entry.getLevel() < 2) {
            node.setIsLeaf(false);
            List<DataEntry> children = entryRepository.findByVersionIdAndParentIdOrderBySortOrder(versionId, entry.getId());
            if (!children.isEmpty()) {
                node.setChildren(children.stream().map(c -> buildTree(c, versionId, catMap, domMap)).toList());
            }
        } else {
            node.setIsLeaf(true);
        }
        return node;
    }

    public List<DataEntry> getChildren(Long versionId, Long parentId, String name, List<String> statusList,
                                      String productManager, String solution, String versionTag) {
        return entryRepository.findByVersionIdAndParentIdWithFilter(
                versionId, parentId, name, productManager, solution, versionTag).stream()
                .filter(e -> matchesStatus(e.getColStatus(), statusList))
                .toList();
    }

    public DataEntry getById(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据条目不存在"));
    }

    @Transactional
    public List<DataEntry> query(Long versionId, Long customTabId, String name, List<String> statusList, String productManager,
                                 String solution, String versionDivision, String bizCategory, String bizDomain, Integer level) {
        boolean hasFilter = (name != null && !name.isEmpty()) || (statusList != null && !statusList.isEmpty())
                || (productManager != null && !productManager.isEmpty()) || (solution != null && !solution.isEmpty())
                || (versionDivision != null && !versionDivision.isEmpty())
                || (bizCategory != null && !bizCategory.isEmpty()) || (bizDomain != null && !bizDomain.isEmpty());

        List<DataEntry> result;
        if (level != null) {
            result = entryRepository.findByVersionIdAndLevel(versionId, level);
            result = sortByCategoryOrder(result, versionId);
            return result.stream().filter(e -> matchesStatus(e.getColStatus(), statusList)).toList();
        }
        if (customTabId != null) {
            if (hasFilter) {
                result = entryRepository.queryEntries(versionId, customTabId, name, productManager,
                        solution, versionDivision, bizCategory, bizDomain);
            } else {
                result = entryRepository.findEntriesByTab(versionId, customTabId);
            }
            result = reorderByCustomTabSort(customTabId, result);
        } else if (hasFilter) {
            result = entryRepository.queryEntries(versionId, null, name, productManager,
                    solution, versionDivision, bizCategory, bizDomain);
        } else if (bizCategory != null || bizDomain != null) {
            result = entryRepository.findEntriesByDomain(versionId, bizCategory, bizDomain);
        } else {
            result = entryRepository.findAllEntries(versionId);
        }
        result = new ArrayList<>(result.stream().filter(e -> matchesStatus(e.getColStatus(), statusList)).toList());
        if (customTabId == null) {
            result = sortByCategoryOrder(result, versionId);
        }
        return result;
    }

    private List<DataEntry> sortByCategoryOrder(List<DataEntry> entries, Long versionId) {
        List<BaseCategory> cats = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
        Map<String, Integer> catOrder = new LinkedHashMap<>();
        for (int i = 0; i < cats.size(); i++) catOrder.put(cats.get(i).getName(), i);
        Map<String, Integer> l2Order = new LinkedHashMap<>();
        List<BaseDomain> domains = baseDomainRepository.findByVersionId(versionId);
        domains.sort(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));
        for (int i = 0; i < domains.size(); i++) l2Order.put(domains.get(i).getName(), i);
        entries.sort(Comparator.comparingInt((DataEntry e) -> catOrder.getOrDefault(e.getColBizCategory(), Integer.MAX_VALUE))
                .thenComparingInt(e -> l2Order.getOrDefault(e.getColBizDomain(), Integer.MAX_VALUE))
                .thenComparingInt(e -> e.getLevel() != null ? e.getLevel() : 3)
                .thenComparing(e -> e.getParentId(), Comparator.nullsLast(Long::compareTo))
                .thenComparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
        return entries;
    }

    @Transactional
    public DataEntry create(DataEntryDTO dto) {
        ensureVersionEditable(dto.getVersionId());

        DataEntry entry = new DataEntry();
        copyFields(entry, dto);
        entry.setVersionId(dto.getVersionId());
        entry.setParentId(dto.getParentId());
        entry.setLevel(dto.getLevel());
        if (dto.getSortOrder() != null) {
            entry.setSortOrder(dto.getSortOrder());
        } else {
            List<DataEntry> siblings = entryRepository.findByVersionIdAndParentId(dto.getVersionId(), dto.getParentId());
            int maxSort = siblings.stream().mapToInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0).max().orElse(-1);
            entry.setSortOrder(maxSort + 1);
        }
        entry.setIsLeaf(true);

        if (dto.getParentId() != null) {
            DataEntry parent = entryRepository.findById(dto.getParentId()).orElse(null);
            if (parent != null && parent.getIsLeaf()) {
                parent.setIsLeaf(false);
                entryRepository.save(parent);
            }
        }

        return entryRepository.save(entry);
    }

    @Transactional
    public DataEntry update(Long id, DataEntryDTO dto) {
        DataEntry entry = getById(id);
        ensureVersionEditable(entry.getVersionId());
        String oldBizCategory = entry.getColBizCategory();
        String oldBizDomain = entry.getColBizDomain();
        copyFields(entry, dto);

        if (entry.getLevel() != null && entry.getLevel() >= 1 && entry.getLevel() <= 3) {
            cascadeLabelUpdate(entry, oldBizCategory, oldBizDomain);
        }

        return entryRepository.save(entry);
    }

    private void cascadeLabelUpdate(DataEntry entry, String oldBizCategory, String oldBizDomain) {
        String newCategory = entry.getColBizCategory();
        String newDomain = entry.getColBizDomain();
        boolean catChanged = newCategory != null && !newCategory.equals(oldBizCategory);
        boolean domChanged = newDomain != null && !newDomain.equals(oldBizDomain);

        if (entry.getLevel() == 1 && catChanged) {
            List<DataEntry> all = entryRepository.findByVersionId(entry.getVersionId());
            for (DataEntry d : all) {
                if (oldBizCategory.equals(d.getColBizCategory())) {
                    d.setColBizCategory(newCategory);
                    entryRepository.save(d);
                }
            }
            List<BaseCategory> cats = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(entry.getVersionId());
            for (BaseCategory c : cats) {
                if (oldBizCategory.equals(c.getName())) {
                    c.setName(newCategory);
                    baseCategoryRepository.save(c);
                    break;
                }
            }
        }

        if (entry.getLevel() == 2 && domChanged) {
            List<DataEntry> all = entryRepository.findByVersionId(entry.getVersionId());
            for (DataEntry d : all) {
                if (oldBizDomain.equals(d.getColBizDomain())) {
                    d.setColBizDomain(newDomain);
                    entryRepository.save(d);
                }
            }
            List<BaseDomain> doms = baseDomainRepository.findByVersionId(entry.getVersionId());
            for (BaseDomain d : doms) {
                if (oldBizDomain.equals(d.getName())) {
                    d.setName(newDomain);
                    baseDomainRepository.save(d);
                    break;
                }
            }
        }

        if (entry.getLevel() == 3 && (catChanged || domChanged)) {
            List<DataEntry> all = entryRepository.findByVersionId(entry.getVersionId());
            List<DataEntry> descendants = collectDescendants(entry.getId(), entry.getVersionId());
            descendants.add(entry);
            for (DataEntry d : descendants) {
                if (catChanged) d.setColBizCategory(newCategory);
                if (domChanged) d.setColBizDomain(newDomain);
                if (d.getId().equals(entry.getId())) continue;
                entryRepository.save(d);
            }
        }
    }

    private List<DataEntry> collectDescendants(Long parentId, Long versionId) {
        List<DataEntry> all = entryRepository.findByVersionId(versionId);
        Map<Long, List<DataEntry>> parentMap = new HashMap<>();
        for (DataEntry e : all) {
            if (e.getParentId() != null) {
                parentMap.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
            }
        }
        List<DataEntry> result = new ArrayList<>();
        java.util.Queue<Long> queue = new java.util.LinkedList<>();
        queue.add(parentId);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            List<DataEntry> children = parentMap.get(pid);
            if (children != null) {
                for (DataEntry child : children) {
                    result.add(child);
                    queue.add(child.getId());
                }
            }
        }
        return result;
    }

    @Transactional
    public void delete(Long id) {
        DataEntry entry = getById(id);
        ensureVersionEditable(entry.getVersionId());
        List<Long> allIds = new ArrayList<>();
        allIds.add(id);
        collectDescendantIds(entry.getVersionId(), id, allIds);
        entryRepository.deleteAllById(allIds);
    }

    @Transactional
    public void batchDelete(List<Long> ids) {
        List<DataEntry> roots = entryRepository.findAllById(ids);
        if (roots.isEmpty()) return;
        Long versionId = roots.get(0).getVersionId();
        Map<Long, List<Long>> parentChildMap = new HashMap<>();
        for (DataEntry e : entryRepository.findByVersionId(versionId)) {
            Long pid = e.getParentId();
            if (pid != null) {
                parentChildMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(e.getId());
            }
        }
        Set<Long> allIds = new LinkedHashSet<>(ids);
        Queue<Long> queue = new LinkedList<>(ids);
        while (!queue.isEmpty()) {
            Long pid = queue.poll();
            List<Long> children = parentChildMap.get(pid);
            if (children != null) {
                for (Long cid : children) {
                    if (allIds.add(cid)) {
                        queue.add(cid);
                    }
                }
            }
        }
        entryRepository.deleteAllByIdInBatch(new ArrayList<>(allIds));
    }

    @Transactional
    public int batchUpdateCategory(Long versionId, List<Long> entryIds, Long categoryId, Long domainId) {
        ensureVersionEditable(versionId);

        String catName = null;
        String domName = null;
        if (categoryId != null) {
            BaseCategory cat = baseCategoryRepository.findById(categoryId).orElse(null);
            if (cat != null) catName = cat.getName();
        }
        if (domainId != null) {
            BaseDomain dom = baseDomainRepository.findById(domainId).orElse(null);
            if (dom != null) domName = dom.getName();
        }

        int count = 0;
        for (Long id : entryIds) {
            DataEntry entry = entryRepository.findById(id).orElse(null);
            if (entry == null) continue;
            if (entry.getLevel() == null || entry.getLevel() < 3) continue;

            if (catName != null) {
                entry.setColBizCategory(catName);
                entry.setCategoryId(categoryId);
            }
            if (domName != null) {
                entry.setColBizDomain(domName);
                entry.setDomainId(domainId);
            }

            List<DataEntry> descendants = collectDescendants(entry.getId(), entry.getVersionId());
            for (DataEntry d : descendants) {
                if (catName != null) d.setColBizCategory(catName);
                if (domName != null) d.setColBizDomain(domName);
                entryRepository.save(d);
            }

            entryRepository.save(entry);
            count++;
        }
        return count;
    }

    private void collectDescendantIds(Long versionId, Long parentId, List<Long> ids) {
        List<DataEntry> children = entryRepository.findByVersionIdAndParentId(versionId, parentId);
        for (DataEntry child : children) {
            ids.add(child.getId());
            collectDescendantIds(versionId, child.getId(), ids);
        }
    }

    public void ensureVersionEditable(Long versionId) {
        DataVersion version = dataVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("版本不存在"));
        if (!"draft".equals(version.getStatus())) {
            throw new BusinessException("已发版版本不允许修改清单");
        }
    }

    private void copyFields(DataEntry entry, DataEntryDTO dto) {
        if (dto.getColProductSystem() != null) entry.setColProductSystem(dto.getColProductSystem());
        if (dto.getColAppRole() != null) entry.setColAppRole(dto.getColAppRole());
        if (dto.getColBidParamDesc() != null) entry.setColBidParamDesc(dto.getColBidParamDesc());
        if (dto.getColFeatureDesc() != null) entry.setColFeatureDesc(dto.getColFeatureDesc());
        if (dto.getColStatus() != null) entry.setColStatus(dto.getColStatus());
        if (dto.getColBizCategory() != null) entry.setColBizCategory(dto.getColBizCategory());
        if (dto.getColBizDomain() != null) entry.setColBizDomain(dto.getColBizDomain());
        if (dto.getCategoryId() != null) entry.setCategoryId(dto.getCategoryId());
        if (dto.getDomainId() != null) entry.setDomainId(dto.getDomainId());
        if (dto.getColVersionDivision() != null) entry.setColVersionDivision(dto.getColVersionDivision());
        if (dto.getColYuan() != null) entry.setColYuan(dto.getColYuan());
        if (dto.getColDeliveryWorkload() != null) entry.setColDeliveryWorkload(dto.getColDeliveryWorkload());
        if (dto.getColControlPoint() != null) entry.setColControlPoint(dto.getColControlPoint());
        if (dto.getColControlPointImg1() != null) entry.setColControlPointImg1(dto.getColControlPointImg1());
        if (dto.getColControlPointImg2() != null) entry.setColControlPointImg2(dto.getColControlPointImg2());
        if (dto.getColControlPointImg3() != null) entry.setColControlPointImg3(dto.getColControlPointImg3());
        if (dto.getColControlPointDoc() != null) entry.setColControlPointDoc(dto.getColControlPointDoc());
        if (dto.getColCopyright() != null) entry.setColCopyright(dto.getColCopyright());
        if (dto.getColRemark() != null) entry.setColRemark(dto.getColRemark());
        if (dto.getColSmartMedical() != null) entry.setColSmartMedical(dto.getColSmartMedical());
        if (dto.getColSmartService() != null) entry.setColSmartService(dto.getColSmartService());
        if (dto.getColSmartManagement() != null) entry.setColSmartManagement(dto.getColSmartManagement());
        if (dto.getColInterconnection() != null) entry.setColInterconnection(dto.getColInterconnection());
        if (dto.getColProductSysId() != null) entry.setColProductSysId(dto.getColProductSysId());
        if (dto.getColModuleId() != null) entry.setColModuleId(dto.getColModuleId());
        if (dto.getColOtherSolutionTag() != null) entry.setColOtherSolutionTag(dto.getColOtherSolutionTag());
        if (dto.getColDocMaintainer() != null) entry.setColDocMaintainer(dto.getColDocMaintainer());
        if (dto.getColProductManager() != null) entry.setColProductManager(dto.getColProductManager());
        if (dto.getColParentRecord() != null) entry.setColParentRecord(dto.getColParentRecord());
        if (dto.getColInternalVersion() != null) entry.setColInternalVersion(dto.getColInternalVersion());
        if (dto.getColIntelligent() != null) entry.setColIntelligent(dto.getColIntelligent());
        if (dto.getColYao() != null) entry.setColYao(dto.getColYao());
        if (dto.getColChi() != null) entry.setColChi(dto.getColChi());
        if (dto.getColFY23() != null) entry.setColFY23(dto.getColFY23());
        if (dto.getColFY24() != null) entry.setColFY24(dto.getColFY24());
        if (dto.getColFY25() != null) entry.setColFY25(dto.getColFY25());
        if (dto.getColFY26() != null) entry.setColFY26(dto.getColFY26());
        if (dto.getColFY27() != null) entry.setColFY27(dto.getColFY27());
        if (dto.getColFY28() != null) entry.setColFY28(dto.getColFY28());
        if (dto.getColFY29() != null) entry.setColFY29(dto.getColFY29());
        if (dto.getColRDCostTotal() != null) entry.setColRDCostTotal(dto.getColRDCostTotal());
        if (dto.getColSalesYao() != null) entry.setColSalesYao(dto.getColSalesYao());
        if (dto.getColSalesYuan() != null) entry.setColSalesYuan(dto.getColSalesYuan());
        if (dto.getColSalesChi() != null) entry.setColSalesChi(dto.getColSalesChi());
        if (dto.getColFactoryPrice() != null) entry.setColFactoryPrice(dto.getColFactoryPrice());
        if (dto.getColPrincipal() != null) entry.setColPrincipal(dto.getColPrincipal());
        if (dto.getColProductLine() != null) entry.setColProductLine(dto.getColProductLine());
        if (dto.getColAssetType() != null) entry.setColAssetType(dto.getColAssetType());
    }

    @Transactional
    public void updateSort(List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            entryRepository.findById(id).ifPresent(entry -> {
                ensureVersionEditable(entry.getVersionId());
                entry.setSortOrder(sortOrder);
                entryRepository.save(entry);
            });
        }
    }

    @Transactional
    public void reorderAll(Long versionId) {
        ensureVersionEditable(versionId);
        List<DataEntry> all = entryRepository.findByVersionId(versionId);

        Map<String, List<DataEntry>> groups = new HashMap<>();
        for (DataEntry e : all) {
            if (e.getLevel() < 3) continue;
            String key = versionId + "_" + e.getLevel() + "_" + (e.getParentId() == null ? 0 : e.getParentId());
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        java.util.Comparator<String> numericPrefixComparator = numericPrefixComparator();

        for (List<DataEntry> group : groups.values()) {
            group.sort((a, b) -> {
                String nameA = a.getColProductSystem() != null ? a.getColProductSystem() : "";
                String nameB = b.getColProductSystem() != null ? b.getColProductSystem() : "";
                return numericPrefixComparator.compare(nameA, nameB);
            });
            for (int i = 0; i < group.size(); i++) {
                group.get(i).setSortOrder(i);
                entryRepository.save(group.get(i));
            }
        }
    }

    private String extractNumberPrefix(String name) {
        if (name == null || name.isEmpty()) return "";
        int end = 0;
        while (end < name.length() && (Character.isDigit(name.charAt(end)) || name.charAt(end) == '.')) {
            end++;
        }
        String prefix = name.substring(0, end);
        while (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private String stripNumberPrefix(String name) {
        if (name == null || name.isEmpty()) return "";
        int end = 0;
        while (end < name.length() && (Character.isDigit(name.charAt(end)) || name.charAt(end) == '.')) {
            end++;
        }
        String rest = name.substring(end).trim();
        return rest;
    }

    @Transactional
    public int dedupByVersion(Long versionId) {
        ensureVersionEditable(versionId);
        return doDedup(versionId, false);
    }

    @Transactional
    public int dedupDeep(Long versionId) {
        ensureVersionEditable(versionId);
        return doDedup(versionId, true);
    }

    private int doDedup(Long versionId, boolean deep) {
        List<DataEntry> all = entryRepository.findByVersionId(versionId);

        Map<String, List<DataEntry>> groups = new HashMap<>();
        for (DataEntry e : all) {
            String name = e.getColProductSystem() != null ? e.getColProductSystem().trim() : "";
            String key;
            if (deep) {
                String coreName = stripNumberPrefix(name).replaceAll("\\s+", "");
                key = e.getLevel() + "_" + coreName;
            } else {
                key = e.getLevel() + "_" + (e.getParentId() == null ? 0 : e.getParentId()) + "_" + name;
            }
            if (key.endsWith("_")) continue;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        int deleted = 0;
        for (List<DataEntry> group : groups.values()) {
            if (group.size() <= 1) continue;
            group.sort((a, b) -> {
                String nameA = a.getColProductSystem() != null ? a.getColProductSystem() : "";
                String nameB = b.getColProductSystem() != null ? b.getColProductSystem() : "";
                return numericPrefixComparator().compare(nameA, nameB);
            });
            DataEntry keeper = group.get(0);
            for (int i = 1; i < group.size(); i++) {
                DataEntry dup = group.get(i);
                entryRepository.findByVersionIdAndParentId(versionId, dup.getId())
                        .forEach(child -> {
                            child.setParentId(keeper.getId());
                            entryRepository.save(child);
                        });
                try {
                    entryRepository.deleteById(dup.getId());
                    deleted++;
                } catch (Exception ignored) {}
            }
        }
        return deleted;
    }

    private java.util.Comparator<String> numericPrefixComparator() {
        return (a, b) -> {
            String pa = extractNumberPrefix(a);
            String pb = extractNumberPrefix(b);
            if (pa.isEmpty() && pb.isEmpty()) return a.compareTo(b);
            if (pa.isEmpty()) return 1;
            if (pb.isEmpty()) return -1;
            String[] partsA = pa.split("\\.");
            String[] partsB = pb.split("\\.");
            int len = Math.min(partsA.length, partsB.length);
            for (int i = 0; i < len; i++) {
                try {
                    int na = Integer.parseInt(partsA[i]);
                    int nb = Integer.parseInt(partsB[i]);
                    if (na != nb) return Integer.compare(na, nb);
                } catch (NumberFormatException e) {
                    int cmp = partsA[i].compareTo(partsB[i]);
                    if (cmp != 0) return cmp;
                }
            }
            return Integer.compare(partsA.length, partsB.length);
        };
    }

    @Transactional
    public void levelUp(Long id) {
        DataEntry entry = getById(id);
        ensureVersionEditable(entry.getVersionId());
        if (entry.getLevel() <= 3) {
            throw new BusinessException("当前层级已是最上层，无法上移");
        }
        DataEntry parent = entryRepository.findById(entry.getParentId())
                .orElseThrow(() -> new BusinessException("父节点不存在"));
        entry.setLevel(entry.getLevel() - 1);
        entry.setParentId(parent.getParentId());
        entryRepository.save(entry);
    }

    @Transactional
    public void levelDown(Long id) {
        DataEntry entry = getById(id);
        ensureVersionEditable(entry.getVersionId());
        Long parentId = entry.getParentId();

        List<DataEntry> siblings = entryRepository.findByVersionIdAndParentIdOrderBySortOrder(
                entry.getVersionId(), parentId);
        DataEntry prevSibling = null;
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(entry.getId()) && i > 0) {
                prevSibling = siblings.get(i - 1);
                break;
            }
        }

        if (prevSibling == null) {
            throw new BusinessException("没有上级同级节点，无法下移");
        }

        entry.setLevel(entry.getLevel() + 1);
        entry.setParentId(prevSibling.getId());
        entryRepository.save(entry);
    }

    private List<DataEntry> reorderByCustomTabSort(Long customTabId, List<DataEntry> entries) {
        List<CustomTabEntry> tabEntries = customTabEntryRepository.findByCustomTabId(customTabId);
        boolean hasNullSort = tabEntries.stream().anyMatch(te -> te.getSortOrder() == null);
        if (hasNullSort) {
            Map<Long, Integer> entrySortMap = new HashMap<>();
            for (DataEntry e : entries) {
                entrySortMap.put(e.getId(), e.getSortOrder() != null ? e.getSortOrder() : 0);
            }
            tabEntries.sort((a, b) -> {
                boolean aHasSort = a.getSortOrder() != null;
                boolean bHasSort = b.getSortOrder() != null;
                if (aHasSort && bHasSort) return Integer.compare(a.getSortOrder(), b.getSortOrder());
                if (aHasSort) return -1;
                if (bHasSort) return 1;
                Integer sortA = entrySortMap.getOrDefault(a.getEntryId(), 0);
                Integer sortB = entrySortMap.getOrDefault(b.getEntryId(), 0);
                if (!sortA.equals(sortB)) return Integer.compare(sortA, sortB);
                return Long.compare(a.getEntryId(), b.getEntryId());
            });
            int order = 0;
            for (CustomTabEntry te : tabEntries) {
                te.setSortOrder(order++);
                customTabEntryRepository.save(te);
            }
        }
        Map<Long, Integer> sortMap = new HashMap<>();
        for (CustomTabEntry te : tabEntries) {
            sortMap.put(te.getEntryId(), te.getSortOrder() != null ? te.getSortOrder() : 0);
        }
        entries.sort((a, b) -> {
            Integer sortA = sortMap.get(a.getId());
            Integer sortB = sortMap.get(b.getId());
            if (sortA == null && sortB == null) return Long.compare(a.getId(), b.getId());
            if (sortA == null) return 1;
            if (sortB == null) return -1;
            if (!sortA.equals(sortB)) return Integer.compare(sortA, sortB);
            return Long.compare(a.getId(), b.getId());
        });
        return entries;
    }

    @Transactional
    public ExcelImportResult importFromExcel(MultipartFile file, Long versionId) {
        ensureVersionEditable(versionId);
        ExcelImportResult result = new ExcelImportResult();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) {
                result.getErrors().add("Excel文件中没有数据行");
                return result;
            }
            result.setTotalRows(lastRow);

            Row headerRow = sheet.getRow(0);
            Map<Integer, String> colMap = new HashMap<>();
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                String header = cell != null ? cell.toString().trim() : "";
                colMap.put(i, header);
                headerMap.put(header, i);
            }

            if (!headerMap.containsKey("招标参数") && headerMap.containsKey("招标参数说明")) {
                headerMap.put("招标参数", headerMap.get("招标参数说明"));
            }
            if (!headerMap.containsKey("招标参数说明") && headerMap.containsKey("招标参数")) {
                headerMap.put("招标参数说明", headerMap.get("招标参数"));
            }
            if (!headerMap.containsKey("解决方案标记") && headerMap.containsKey("其他解决方案标记")) {
                headerMap.put("解决方案标记", headerMap.get("其他解决方案标记"));
            }
            if (!headerMap.containsKey("其他解决方案标记") && headerMap.containsKey("解决方案标记")) {
                headerMap.put("其他解决方案标记", headerMap.get("解决方案标记"));
            }

            Map<String, Long> catNameToId = new HashMap<>();
            Map<Long, String> catIdToName = new HashMap<>();
            for (BaseCategory cat : baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId)) {
                catNameToId.put(cat.getName(), cat.getId());
                catNameToId.put(cat.getName().replaceAll("(\\d+\\.)\\s+", "$1"), cat.getId());
                catIdToName.put(cat.getId(), cat.getName());
            }
            Map<String, Long> domNameToId = new HashMap<>();
            Map<Long, String> domIdToName = new HashMap<>();
            for (BaseDomain dom : baseDomainRepository.findByVersionId(versionId)) {
                domNameToId.put(dom.getName(), dom.getId());
                domNameToId.put(dom.getName().replaceAll("(\\d+\\.)\\s+", "$1"), dom.getId());
                domIdToName.put(dom.getId(), dom.getName());
            }

            List<RowData> rows = new ArrayList<>();
            for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                String productName = getCellString(row, headerMap.getOrDefault("产品/系统", -1));
                productName = cleanMultiline(productName);
                if (productName == null || productName.isEmpty()) continue;
                String parentName = getCellString(row, headerMap.getOrDefault("父记录", -1));
                parentName = cleanMultiline(parentName);
                rows.add(new RowData(rowIdx, row, productName, parentName));
            }

            Map<String, Long> nameToId = new HashMap<>();
            List<RowData> retryRows = new ArrayList<>();
            for (RowData rd : rows) {
                if (rd.parentName == null) {
                    processRow(rd, headerMap, versionId, result, nameToId, catNameToId, domNameToId, catIdToName, domIdToName);
                } else {
                    retryRows.add(rd);
                }
            }

            retryRows.sort((a, b) -> {
                int depthA = a.productName.split("\\.").length;
                int depthB = b.productName.split("\\.").length;
                return Integer.compare(depthA, depthB);
            });

            for (RowData rd : retryRows) {
                processRow(rd, headerMap, versionId, result, nameToId, catNameToId, domNameToId, catIdToName, domIdToName);
            }
        } catch (Exception e) {
            result.getErrors().add("解析Excel文件失败: " + e.getMessage());
        }
        return result;
    }

    private void processRow(RowData rd, Map<String, Integer> headerMap, Long versionId,
                            ExcelImportResult result, Map<String, Long> nameToId,
                            Map<String, Long> catNameToId, Map<String, Long> domNameToId,
                            Map<Long, String> catIdToName, Map<Long, String> domIdToName) {
        try {
            Row row = rd.row;
            String productName = rd.productName;
            String parentName = rd.parentName;
            int rowIdx = rd.rowIdx;
            String bizCategory = getCellString(row, headerMap.getOrDefault("业务分类", -1));
            String bizDomain = getCellString(row, headerMap.getOrDefault("业务域", -1));

            Long parentId = null;
            int level = 3;

            if (parentName != null) {
                parentId = nameToId.get(parentName);
                if (parentId == null) {
                    List<DataEntry> parents = entryRepository
                        .findByVersionIdAndColProductSystem(versionId, parentName);
                    if (!parents.isEmpty()) {
                        parentId = parents.get(0).getId();
                        nameToId.put(parentName, parentId);
                        level = (parents.get(0).getLevel() != null ? parents.get(0).getLevel() : 3) + 1;
                    }
                } else {
                    DataEntry p = entryRepository.findById(parentId).orElse(null);
                    if (p != null) {
                        level = (p.getLevel() != null ? p.getLevel() : 3) + 1;
                    }
                }
            }

            if (parentName != null && parentId == null) {
                result.getErrors().add("行" + (rowIdx + 1) + ": 找不到父记录 '" + parentName + "'");
                result.setFailRows(result.getFailRows() + 1);
                return;
            }

            DataEntry existing = null;
            List<DataEntry> matches;
            final Long finalParentId = parentId;
            if (parentId != null) {
                matches = entryRepository.findByVersionIdAndColBizCategoryAndColBizDomainAndColProductSystem(
                    versionId, bizCategory, bizDomain, productName);
                existing = matches.stream()
                    .filter(e -> finalParentId.equals(e.getParentId()))
                    .findFirst().orElse(null);
            } else {
                matches = entryRepository.findByVersionIdAndColBizCategoryAndColBizDomainAndColProductSystem(
                    versionId, bizCategory, bizDomain, productName);
                if (!matches.isEmpty()) existing = matches.get(0);
            }

            boolean isUpdate = existing != null;
            DataEntry entry = isUpdate ? existing : new DataEntry();

            if (!isUpdate) {
                entry.setVersionId(versionId);
                entry.setParentId(parentId);
                entry.setLevel(level);
                entry.setSortOrder(0);
                entry.setIsLeaf(true);
            }

            setStr(headerMap, row, entry, "应用角色", DataEntry::setColAppRole);
            setStr(headerMap, row, entry, "招标参数", DataEntry::setColBidParamDesc);
            setStr(headerMap, row, entry, "功能说明", DataEntry::setColFeatureDesc);
            setStr(headerMap, row, entry, "状态", DataEntry::setColStatus);
            Long catId = bizCategory != null ? catNameToId.get(bizCategory) : null;
            Long domId = bizDomain != null ? domNameToId.get(bizDomain) : null;
            entry.setColBizCategory(catId != null ? catIdToName.get(catId) : bizCategory);
            entry.setColBizDomain(domId != null ? domIdToName.get(domId) : bizDomain);
            entry.setColProductSystem(productName);
            entry.setCategoryId(catId);
            entry.setDomainId(domId);
            setStr(headerMap, row, entry, "版本划分", DataEntry::setColVersionDivision);
            setStr(headerMap, row, entry, "远", DataEntry::setColYuan);
            setStr(headerMap, row, entry, "交付工作量(人月)", DataEntry::setColDeliveryWorkload);
            setStr(headerMap, row, entry, "控标点", DataEntry::setColControlPoint);
            setStr(headerMap, row, entry, "控标点截图-1", DataEntry::setColControlPointImg1);
            setStr(headerMap, row, entry, "控标点截图-2", DataEntry::setColControlPointImg2);
            setStr(headerMap, row, entry, "控标点截图-3", DataEntry::setColControlPointImg3);
            setStr(headerMap, row, entry, "控标点文档说明", DataEntry::setColControlPointDoc);
            setStr(headerMap, row, entry, "软著", DataEntry::setColCopyright);
            setStr(headerMap, row, entry, "备注", DataEntry::setColRemark);
            setStr(headerMap, row, entry, "智慧医疗", DataEntry::setColSmartMedical);
            setStr(headerMap, row, entry, "智慧服务", DataEntry::setColSmartService);
            setStr(headerMap, row, entry, "智慧管理", DataEntry::setColSmartManagement);
            setStr(headerMap, row, entry, "互联互通", DataEntry::setColInterconnection);
            setStr(headerMap, row, entry, "产品/系统标识", DataEntry::setColProductSysId);
            setStr(headerMap, row, entry, "模块标识", DataEntry::setColModuleId);
            setStr(headerMap, row, entry, "解决方案标记", DataEntry::setColOtherSolutionTag);
            setStr(headerMap, row, entry, "文档维护人员", DataEntry::setColDocMaintainer);
            setStr(headerMap, row, entry, "产品经理", DataEntry::setColProductManager);
            if (entry.getColProductManager() == null || entry.getColProductManager().isBlank()) {
                entry.setColProductManager("未指定");
            }
            setStr(headerMap, row, entry, "内部版本", DataEntry::setColInternalVersion);
            setStr(headerMap, row, entry, "智能化", DataEntry::setColIntelligent);
            setStr(headerMap, row, entry, "曜", DataEntry::setColYao);
            setStr(headerMap, row, entry, "驰", DataEntry::setColChi);
            setStr(headerMap, row, entry, "负责人", DataEntry::setColPrincipal);
            setStr(headerMap, row, entry, "产品线", DataEntry::setColProductLine);
            setStr(headerMap, row, entry, "资产类型", DataEntry::setColAssetType);
            setBd(headerMap, row, entry, "FY23", DataEntry::setColFY23);
            setBd(headerMap, row, entry, "FY24", DataEntry::setColFY24);
            setBd(headerMap, row, entry, "FY25", DataEntry::setColFY25);
            setBd(headerMap, row, entry, "FY26", DataEntry::setColFY26);
            setBd(headerMap, row, entry, "FY27", DataEntry::setColFY27);
            setBd(headerMap, row, entry, "FY28", DataEntry::setColFY28);
            setBd(headerMap, row, entry, "FY29", DataEntry::setColFY29);
            setBd(headerMap, row, entry, "研发成本合计", DataEntry::setColRDCostTotal);
            setBd(headerMap, row, entry, "出厂套价-保本", DataEntry::setColFactoryPrice);
            setIntVal(headerMap, row, entry, "销量-曜", DataEntry::setColSalesYao);
            setIntVal(headerMap, row, entry, "销量-远", DataEntry::setColSalesYuan);
            setIntVal(headerMap, row, entry, "销量-驰", DataEntry::setColSalesChi);

            String feat = entry.getColFeatureDesc();
            if (feat != null) {
                entry.setColFeatureDesc(feat.replaceAll("<(https?://[^>]+)>", "[$1]"));
            }

            entryRepository.save(entry);
            nameToId.put(productName, entry.getId());

            if (parentId != null) {
                DataEntry parent = entryRepository.findById(parentId).orElse(null);
                if (parent != null && parent.getIsLeaf()) {
                    parent.setIsLeaf(false);
                    entryRepository.save(parent);
                }
            }

            if (isUpdate) {
                result.setUpdateRows(result.getUpdateRows() + 1);
            }
            result.setSuccessRows(result.getSuccessRows() + 1);
        } catch (Exception e) {
            result.getErrors().add("行" + (rd.rowIdx + 1) + ": " + e.getMessage());
            result.setFailRows(result.getFailRows() + 1);
        }
    }

    private static class RowData {
        final int rowIdx;
        final Row row;
        final String productName;
        final String parentName;
        RowData(int rowIdx, Row row, String productName, String parentName) {
            this.rowIdx = rowIdx;
            this.row = row;
            this.productName = productName;
            this.parentName = parentName;
        }
    }

    private String cleanMultiline(String val) {
        if (val == null) return null;
        val = val.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        val = val.replaceAll("(\\d)\\s+\\.\\s+(\\d)", "$1.$2");
        val = val.replaceAll("(\\d)\\.\\s+(\\d)", "$1.$2");
        val = val.replaceAll("(\\d)\\s+\\.(\\d)", "$1.$2");
        val = val.replaceAll("\\.\\s+(\\d)", ".$1");
        val = val.replaceAll("(\\d)\\s+\\.", "$1.");
        val = val.replaceAll("(\\d)\\s+(\\d)(?=\\s*[\\u4e00-\\u9fff])", "$1$2");
        return val.isEmpty() ? null : val;
    }

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private String getCellString(Row row, int colIdx) {
        if (colIdx < 0) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        String val = DATA_FORMATTER.formatCellValue(cell);
        return val != null && !val.trim().isEmpty() ? val.trim() : null;
    }

    private void setStr(Map<String, Integer> hm, Row row, DataEntry e, String h, java.util.function.BiConsumer<DataEntry, String> setter) {
        Integer idx = hm.get(h);
        if (idx == null) return;
        String val = getCellString(row, idx);
        if (val != null && !val.isEmpty()) setter.accept(e, val);
    }

    private void setBd(Map<String, Integer> hm, Row row, DataEntry e, String h, java.util.function.BiConsumer<DataEntry, BigDecimal> setter) {
        Integer idx = hm.get(h);
        if (idx == null) return;
        String val = getCellString(row, idx);
        if (val != null && !val.isEmpty()) {
            try { setter.accept(e, new BigDecimal(val)); } catch (NumberFormatException ignored) {}
        }
    }

    private void setIntVal(Map<String, Integer> hm, Row row, DataEntry e, String h, java.util.function.BiConsumer<DataEntry, Integer> setter) {
        Integer idx = hm.get(h);
        if (idx == null) return;
        String val = getCellString(row, idx);
        if (val != null && !val.isEmpty()) {
            try { setter.accept(e, Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
        }
    }

    public String getPreviewHtml(Long entryId, boolean isEditing, String username, String mode) {
        return getPreviewHtml(List.of(entryId), isEditing, username, mode);
    }

    public String getPreviewHtml(List<Long> entryIds, boolean isEditing, String username, String mode) {
        String roleCode = "USER";
        if (username != null) {
            com.superpower.modules.system.entity.SysUser user = null;
            try { user = findUserByUsername(username); } catch (Exception ignored) {}
            if (user != null && user.getRole() != null) roleCode = user.getRole().getCode();
        }
        String role = mapRoleCode(roleCode);

        Map<Long, String> rejectReasons = new HashMap<>();
        StringBuilder nav = new StringBuilder();
        StringBuilder body = new StringBuilder();

        for (Long entryId : entryIds) {
            DataEntry l3 = entryRepository.findById(entryId).orElse(null);
            if (l3 == null) continue;
            List<DataEntry> all = collectL3AndDescendants(l3);
            for (DataEntry e : all) {
                if ("驳回".equals(e.getApprovalStatus())) {
                    List<ApprovalLog> logs = approvalLogRepository.findByEntryIdOrderByCreatedAtDesc(e.getId());
                    for (ApprovalLog log : logs) {
                        if ("reject".equals(log.getAction())) { rejectReasons.put(e.getId(), log.getComment()); break; }
                    }
                }
            }
            Map<Long, List<DataEntry>> childrenMap = new HashMap<>();
            for (DataEntry e : all) {
                if (!e.getId().equals(l3.getId())) {
                    childrenMap.computeIfAbsent(e.getParentId() != null ? e.getParentId() : l3.getId(), k -> new ArrayList<>()).add(e);
                }
            }
            for (List<DataEntry> list : childrenMap.values()) {
                list.sort(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));
            }
            buildNavAndBody(l3, childrenMap, nav, body, 0, rejectReasons, isEditing, role, mode);
        }

        return buildPreviewHtmlWrapper(nav, body);
    }

    private String buildPreviewHtmlWrapper(StringBuilder nav, StringBuilder body) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>"
            + "*{margin:0;padding:0;box-sizing:border-box;}"
            + "body{font-family:'SimSun','宋体',serif;font-size:10.5pt;line-height:1.5;color:#000;display:flex;flex-direction:column;height:100vh;overflow:hidden;}"
            + ".layout{display:flex;flex:1;overflow:hidden;}"
            + ".sidebar{width:260px;flex-shrink:0;overflow-y:auto;border-right:1px solid #e2e8f0;background:#f8fafc;padding:12px 0;font-family:'Microsoft YaHei',sans-serif;}"
            + ".sidebar a{display:block;padding:6px 12px 6px 16px;color:#334155;text-decoration:none;font-size:13px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;cursor:pointer;}"
            + ".sidebar a:hover{background:#e2e8f0;color:#1e293b;}"
            + ".sidebar a.active{background:#dbeafe;color:#2563eb;font-weight:600;}"
            + ".sidebar .lvl0{padding-left:8px;font-weight:600;}"
            + ".sidebar .lvl1{padding-left:24px;}"
            + ".sidebar .lvl2{padding-left:40px;}"
            + ".sidebar .lvl3{padding-left:56px;}"
            + ".sidebar .lvl4{padding-left:72px;}"
            + ".content{flex:1;overflow-y:auto;padding:36px 48px;}"
            + ".layout{display:flex;flex:1;overflow:hidden;}"
            + "h3{font-size:16pt;font-weight:bold;margin:12pt 0 6pt;line-height:1.5;scroll-margin-top:12px;display:flex;align-items:center;}"
            + ".p{text-indent:2em;margin:0;line-height:1.5;font-size:10.5pt;}"
            + ".img-wrap{text-align:center;margin:6pt 0;}"
            + ".img-wrap img{max-width:100%;max-height:400px;}"
            + ".img-caption{text-align:center;font-size:9pt;color:#666;margin-top:2pt;}"
            + ".img-grid{display:flex;flex-wrap:wrap;gap:8px;justify-content:center;margin:6pt 0;}"
            + ".img-grid-cell{flex:0 0 calc(33.33% - 6px);max-width:calc(33.33% - 6px);min-width:120px;text-align:center;box-sizing:border-box;}"
            + ".img-grid-cell img{width:auto;height:300px;max-width:100%;object-fit:contain;}"
            + ".toggle{display:inline-block;width:18px;height:18px;line-height:18px;text-align:center;cursor:pointer;margin-right:2px;user-select:none;font-size:10px;background:#e2e8f0;border-radius:3px;color:#475569;font-weight:bold;}"
            + ".toggle-empty{visibility:hidden;cursor:default;}"
            + ".entry-actions{padding:10px 12px;border:1px solid #e2e8f0;border-radius:8px;margin-top:10px;display:flex;gap:6px;flex-wrap:wrap;align-items:center;background:#fafbfc;}"
            + ".ea-label{font-size:9pt;color:#94a3b8;margin-right:2px;white-space:nowrap;}"
            + ".ea-btn{font-size:10pt;color:#409eff;text-decoration:none;cursor:pointer;padding:3px 10px;border:1px solid #d9ecff;border-radius:4px;background:#ecf5ff;}"
            + ".ea-btn:hover{background:#d9ecff;}"
            + ".ea-btn-success{color:#67c23a;border-color:#e1f3d8;background:#f0f9eb;}"
            + ".ea-btn-success:hover{background:#e1f3d8;}"
            + ".ea-btn-warning{color:#e6a23c;border-color:#faecd8;background:#fdf6ec;}"
            + ".ea-btn-warning:hover{background:#faecd8;}"
            + ".ea-btn-danger{color:#f56c6c;border-color:#fde2e2;background:#fef0f0;}"
            + ".ea-btn-danger:hover{background:#fde2e2;}"
            + "@keyframes highlightFlash{0%{background-color:#d4edda;box-shadow:0 0 12px rgba(103,194,58,0.3)}30%{background-color:#d4edda}100%{background-color:transparent;box-shadow:none}}"
            + ".nav-dot{display:inline-block;width:6px;height:6px;border-radius:50%;margin-right:4px;vertical-align:middle;position:relative;top:-1px;}"
            + ".legend-bar{display:flex;align-items:center;gap:12px;padding:6px 12px;background:#f1f5f9;border-bottom:1px solid #e2e8f0;font-size:11px;color:#64748b;font-family:'Microsoft YaHei',sans-serif;}"
            + ".legend-item{display:flex;align-items:center;gap:3px;}"
            + ".legend-dot{display:inline-block;width:6px;height:6px;border-radius:50%;}"
            + "</style></head><body>"
            + "<div class='legend-bar'>"
            + "<span style='font-weight:600;color:#475569;margin-right:4px;'>审批状态：</span>"
            + "<span class='legend-item'><span class='legend-dot' style='background:#409eff;'></span>待提交</span>"
            + "<span class='legend-item'><span class='legend-dot' style='background:#e6a23c;'></span>待审批</span>"
            + "<span class='legend-item'><span class='legend-dot' style='background:#f56c6c;'></span>驳回</span>"
            + "<span class='legend-item'><span class='legend-dot' style='background:#67c23a;'></span>通过</span>"
            + "</div>"
            + "<div class='layout'>"
            + "<div class='sidebar' id='sidebar'>" + nav + "</div>"
            + "<div class='content' id='content'>" + body + "</div>"
            + "</div><script>"
            + "function scrollToAnchor(id){var el=document.getElementById(id);if(el)el.scrollIntoView({behavior:'instant',block:'start'});}"
            + "document.querySelectorAll('.toggle').forEach(t=>{t.onclick=function(e){e.preventDefault();e.stopPropagation();var p=t.parentElement;var ch=p.parentElement.querySelector('div');if(ch){ch.style.display=ch.style.display==='none'?'block':'none';t.textContent=ch.style.display==='none'?'+':'-'}}});"
            + "window.addEventListener('message',function(e){"
            + "var m=e.data;if(m.action!=='highlightEntry')return;"
            + "var h3=document.getElementById('e'+m.entryId);if(!h3)return;"
            + "h3.scrollIntoView({behavior:'smooth',block:'center'});"
            + "h3.style.animation='highlightFlash 3s ease-out';"
            + "setTimeout(function(){h3.style.animation=''},3000);"
            + "});"
            + "</script></body></html>";
    }

    private void buildNavAndBody(DataEntry node, Map<Long, List<DataEntry>> childrenMap,
                                  StringBuilder nav, StringBuilder body, int depth,
                                  Map<Long, String> rejectReasons, boolean isEditing, String role, String mode) {
        String nodeId = "e" + node.getId();
        String label = node.getColProductSystem() != null ? node.getColProductSystem() : "";
        String colStatus = node.getColStatus();
        String nodeApprovalStatus = node.getApprovalStatus();
        List<DataEntry> children = childrenMap.getOrDefault(node.getId(), new ArrayList<>());
        children.sort(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));
        boolean hasChildren = !children.isEmpty();
        nav.append("<div>");
        nav.append("<a href='javascript:void(0)' onclick=\"scrollToAnchor('").append(nodeId).append("')\" class='lvl").append(depth).append("'>");
        if (hasChildren) {
            nav.append("<span class='toggle'>-</span>");
        } else {
            nav.append("<span class='toggle toggle-empty'></span>");
        }
        if (nodeApprovalStatus != null && !nodeApprovalStatus.isEmpty() && colStatus != null && colStatus.contains("可交付") && node.getLevel() != null && node.getLevel() >= 3) {
            nav.append("<span class='nav-dot' style='background:").append(getApprovalDotColor(nodeApprovalStatus)).append("'></span>");
        }
        nav.append(label).append("</a>");
        body.append("<h3 id='").append(nodeId).append("' style='display:flex;align-items:center;'>")
            .append("<span>").append(label).append("</span>");
        String productManager = node.getColProductManager();
        if (productManager != null && !productManager.isEmpty()) {
            body.append("<span style='font-size:10pt;color:#909399;margin-left:8px;'>（").append(productManager).append("）</span>");
        }
        String approvalStatus = node.getApprovalStatus();
        if (approvalStatus != null && !approvalStatus.isEmpty() && colStatus != null && colStatus.contains("可交付")) {
            body.append("<span style='").append(getApprovalTagStyle(approvalStatus)).append("cursor:pointer;' onclick=\"parent.postMessage({action:'showLogs',entryId:").append(node.getId()).append("},'*')\">").append(approvalStatus).append("</span>");
            if ("驳回".equals(approvalStatus)) {
                String reason = rejectReasons.get(node.getId());
                if (reason != null && !reason.isEmpty()) {
                    body.append("<span style='color:#f56c6c;font-size:10pt;margin-left:6px;'>原因：").append(reason).append("</span>");
                }
            }
        }
        body.append("</h3>");
        String desc = "bid".equals(mode) ? node.getColBidParamDesc() : node.getColFeatureDesc();
        if (desc != null && !desc.isBlank()) {
            body.append(toPreviewParagraphs(desc));
        }
        if (isEditing) {
            body.append("<div class='entry-actions' data-entry-id='").append(node.getId()).append("'>");
            body.append("<div class='ea-label'>数据操作：</div>");
            body.append("<a class='ea-btn' onclick=\"parent.postMessage({action:'edit',entryId:").append(node.getId()).append("},'*')\">编辑</a>");
            if (node.getLevel() != null && node.getLevel() >= 3) {
                body.append("<a class='ea-btn' onclick=\"parent.postMessage({action:'addChild',entryId:").append(node.getId()).append("},'*')\">添加</a>");
            }
            body.append("<a class='ea-btn ea-btn-danger' onclick=\"parent.postMessage({action:'delete',entryId:").append(node.getId()).append(",entryName:'").append(label.replace("'", "\\'")).append("'},'*')\">删除</a>");
            String apprStatus = node.getApprovalStatus();
            boolean showApproval = isEditing && colStatus != null && colStatus.contains("可交付");
            if (showApproval) {
                body.append("<div class='ea-label' style='margin-left:16px;'>流程操作：</div>");
                boolean canSubmit = (apprStatus == null || apprStatus.isEmpty() || "待提交".equals(apprStatus) || "驳回".equals(apprStatus));
                boolean canWithdraw = "待审核".equals(apprStatus);
                boolean canApprove = "待审核".equals(apprStatus);
                boolean canReject = "待审核".equals(apprStatus) || "审核通过".equals(apprStatus);
                if (("editor".equals(role) || "admin".equals(role)) && canSubmit) {
                    body.append("<a class='ea-btn ea-btn-warning' onclick=\"parent.postMessage({action:'submit',entryId:").append(node.getId()).append("},'*')\">提交</a>");
                }
                if (("editor".equals(role) || "admin".equals(role)) && canWithdraw) {
                    body.append("<a class='ea-btn' onclick=\"parent.postMessage({action:'withdraw',entryId:").append(node.getId()).append("},'*')\">撤销</a>");
                }
                if (("reviewer".equals(role) || "admin".equals(role)) && canApprove) {
                    body.append("<a class='ea-btn ea-btn-success' onclick=\"parent.postMessage({action:'approve',entryId:").append(node.getId()).append("},'*')\">通过</a>");
                }
                if (("reviewer".equals(role) || "admin".equals(role)) && canReject) {
                    body.append("<a class='ea-btn ea-btn-danger' onclick=\"parent.postMessage({action:'reject',entryId:").append(node.getId()).append("},'*')\">驳回</a>");
                }
            }
            body.append("</div>");
        }
        if (hasChildren) {
            nav.append("<div>");
            for (DataEntry child : children) {
                buildNavAndBody(child, childrenMap, nav, body, depth + 1, rejectReasons, isEditing, role, mode);
            }
            nav.append("</div>");
        }
        nav.append("</div>");
    }

    private String toPreviewParagraphs(String desc) {
        String cleaned = cleanImageCardsToText(desc);
        cleaned = cleaned.replace("\r\n", "\n").replace('\r', '\n');
        StringBuilder sb = new StringBuilder();
        Pattern urlPattern = Pattern.compile("https?://[^\\s\\[\\]|]+");
        Pattern urlLinePattern = Pattern.compile("^https?://[^\\s\\[\\]|]+(?:\\|[^\\s]*)?$");

        String[] lines = cleaned.split("\n");
        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();
            if (line.isEmpty()) { i++; continue; }

            List<String> batch = new ArrayList<>();
            while (i < lines.length) {
                String l = lines[i].trim();
                if (l.isEmpty()) { i++; continue; }
                if (urlLinePattern.matcher(l).matches()) {
                    batch.add(l);
                    i++;
                } else {
                    break;
                }
            }

            if (!batch.isEmpty()) {
                if (batch.size() >= 2) {
                    List<String> encUrls = new ArrayList<>();
                    List<String> captions = new ArrayList<>();
                    for (String raw : batch) {
                        int pi = raw.indexOf('|');
                        String urlPart = pi > 0 ? raw.substring(0, pi) : raw;
                        String cap = pi > 0 ? raw.substring(pi + 1) : "";
                        encUrls.add(encodeUrl(urlPart));
                        captions.add(cap);
                    }
                    boolean allPortrait = true;
                    for (String raw : batch) {
                        int pi = raw.indexOf('|');
                        String urlPart = pi > 0 ? raw.substring(0, pi) : raw;
                        int[] wh = getImageDimensions(urlPart);
                        if (wh[0] > 0 && wh[1] > 0) {
                            if ((double) wh[1] / wh[0] <= 1.2) allPortrait = false;
                        } else {
                            allPortrait = false;
                        }
                    }
                    if (allPortrait) {
                        sb.append("<div class='img-grid'>");
                        for (int k = 0; k < encUrls.size(); k++) {
                            sb.append("<div class='img-grid-cell'>")
                              .append("<img src='").append(encUrls.get(k)).append("' style='height:300px;' onerror=\"this.onerror=null;this.src='http://localhost:8080/api/images/file/error.png';\" />");
                            String cap = captions.get(k);
                            if (!cap.isEmpty()) sb.append("<div class='img-caption'>图：").append(cap).append("</div>");
                            else sb.append("<div class='img-caption'></div>");
                            sb.append("</div>");
                        }
                        sb.append("</div>");
                    } else {
                        for (int k = 0; k < encUrls.size(); k++) {
                            sb.append("<div class='img-wrap'>")
                              .append("<img src='").append(encUrls.get(k)).append("' onerror=\"this.onerror=null;this.src='http://localhost:8080/api/images/file/error.png';this.parentElement.querySelector('.img-caption').textContent='缺失图片'\" />");
                            String cap = captions.get(k);
                            if (!cap.isEmpty()) sb.append("<div class='img-caption'>图：").append(cap).append("</div>");
                            else sb.append("<div class='img-caption'></div>");
                            sb.append("</div>");
                        }
                    }
                } else {
                    String raw = batch.get(0);
                    int pi = raw.indexOf('|');
                    String urlPart = pi > 0 ? raw.substring(0, pi) : raw;
                    String cap = pi > 0 ? raw.substring(pi + 1) : "";
                    String enc = encodeUrl(urlPart);
                    sb.append("<div class='img-wrap'>")
                      .append("<img src='").append(enc).append("' onerror=\"this.onerror=null;this.src='http://localhost:8080/api/images/file/error.png';this.parentElement.querySelector('.img-caption').textContent='缺失图片'\" />");
                    if (!cap.isEmpty()) sb.append("<div class='img-caption'>图：").append(cap).append("</div>");
                    else sb.append("<div class='img-caption'></div>");
                    sb.append("</div>");
                }
                continue;
            }

            Matcher um = urlPattern.matcher(line);
            List<String> inlineUrls = new ArrayList<>();
            while (um.find()) inlineUrls.add(um.group());
            if (!inlineUrls.isEmpty()) {
                int lastEnd = 0;
                um.reset();
                while (um.find()) {
                    if (um.start() > lastEnd) {
                        String textBefore = line.substring(lastEnd, um.start());
                        if (!textBefore.isEmpty()) sb.append("<p class='p'>").append(textBefore.replace("<", "&lt;").replace(">", "&gt;")).append("</p>");
                    }
                    String rawUrl = um.group();
                    String enc = encodeUrl(rawUrl);
                    sb.append("<div class='img-wrap'>")
                      .append("<img src='").append(enc).append("' onerror=\"this.onerror=null;this.src='http://localhost:8080/api/images/file/error.png';this.parentElement.querySelector('.img-caption').textContent='缺失图片'\" />")
                      .append("<div class='img-caption'></div></div>");
                    lastEnd = um.end();
                }
                if (lastEnd < line.length()) {
                    String textAfter = line.substring(lastEnd);
                    if (!textAfter.isEmpty()) sb.append("<p class='p'>").append(textAfter.replace("<", "&lt;").replace(">", "&gt;")).append("</p>");
                }
            } else {
                sb.append("<p class='p'>").append(line.replace("<", "&lt;").replace(">", "&gt;")).append("</p>");
            }
            i++;
        }
        return sb.toString();
    }

    private int[] getImageDimensions(String rawUrl) {
        try {
            String url = rawUrl;
            int hashIdx = url.indexOf('#');
            if (hashIdx > 0) url = url.substring(0, hashIdx);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() != 200) return new int[]{0, 0};
            byte[] data = conn.getInputStream().readAllBytes();
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) return new int[]{0, 0};
            return new int[]{img.getWidth(), img.getHeight()};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    private void appendTextLines(StringBuilder sb, String text) {
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                sb.append("<p class='p'>").append(trimmed.replace("<", "&lt;").replace(">", "&gt;")).append("</p>");
            }
        }
    }

    private String cleanImageCardsToText(String html) {
        Pattern cardPattern = Pattern.compile("<(?:span|div)\\s+class=\"(?:image-card|img-card)\"[^>]*>", Pattern.DOTALL);
        Pattern urlPattern = Pattern.compile("data-url=\"([^\"]+)\"");
        StringBuilder result = new StringBuilder();
        Matcher cm = cardPattern.matcher(html);
        int lastEnd = 0;
        Pattern namePattern = Pattern.compile("data-filename=\"([^\"]*)\"");
        while (cm.find()) {
            int cardStart = cm.start();
            result.append(html, lastEnd, cardStart);
            String openTag = cm.group();
            String url = null;
            Matcher um = urlPattern.matcher(openTag);
            if (um.find()) url = um.group(1);
            if (url == null) { result.append(openTag); lastEnd = cm.end(); continue; }
            String filename = null;
            Matcher nm = namePattern.matcher(openTag);
            if (nm.find()) filename = nm.group(1);
            if (url.startsWith("/api/images/file/")) url = "http://localhost:8080" + url;
            String tagName = openTag.startsWith("<div") ? "div" : "span";
            int depth = 1, pos = cm.end(), contentEnd = pos;
            while (pos < html.length() && depth > 0) {
                int nextOpen = html.indexOf("<" + tagName, pos);
                if (nextOpen >= 0 && nextOpen < html.length() - tagName.length() - 1) {
                    char after = html.charAt(nextOpen + 1 + tagName.length());
                    if (!(Character.isWhitespace(after) || after == '>')) nextOpen = -1;
                }
                int nextClose = html.indexOf("</" + tagName + ">", pos);
                if (nextClose < 0) break;
                if (nextOpen >= 0 && nextOpen < nextClose) { depth++; pos = nextOpen + 1; }
                else { depth--; pos = nextClose + tagName.length() + 3; if (depth == 0) contentEnd = pos; }
            }
            result.append(encodeUrl(url));
            if (filename != null && !filename.isEmpty()) result.append("|").append(filename);
            result.append("\n");
            lastEnd = contentEnd;
        }
        result.append(html, lastEnd, html.length());
        return result.toString().replaceAll("<[^>]+>", "");
    }

    private String encodeUrl(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) return url;
        String scheme = url.substring(0, schemeEnd);
        String rest = url.substring(schemeEnd + 3);
        int pathStart = rest.indexOf('/');
        if (pathStart < 0) return url;
        String hostPort = rest.substring(0, pathStart);
        String pathQuery = rest.substring(pathStart);
        try {
            String[] segments = pathQuery.split("/", -1);
            StringBuilder sb = new StringBuilder();
            for (String seg : segments) {
                if (seg.isEmpty()) continue;
                String decoded;
                try {
                    decoded = java.net.URLDecoder.decode(seg, "UTF-8");
                } catch (Exception e) {
                    decoded = seg;
                }
                sb.append("/").append(java.net.URLEncoder.encode(decoded, "UTF-8").replace("+", "%20"));
            }
            return scheme + "://" + hostPort + sb;
        } catch (Exception e) {
            return url;
        }
    }

    private List<DataEntry> collectL3AndDescendants(DataEntry l3) {
        List<DataEntry> result = new ArrayList<>();
        addNodeAndChildren(l3, result);
        return result;
    }

    private String getApprovalTagStyle(String status) {
        String color = switch (status) {
            case "待提交" -> "#409eff";
            case "待审核" -> "#e6a23c";
            case "审核通过" -> "#67c23a";
            case "驳回" -> "#f56c6c";
            default -> "#909399";
        };
        return "display:inline-block;font-size:10pt;vertical-align:baseline;margin-left:8px;padding:2px 8px;border-radius:3px;background:" + color + "22;color:" + color + ";border:1px solid " + color + "44;line-height:1;position:relative;top:-1px;";
    }

    private String getApprovalDotColor(String status) {
        return switch (status) {
            case "待提交" -> "#409eff";
            case "待审核" -> "#e6a23c";
            case "审核通过" -> "#67c23a";
            case "驳回" -> "#f56c6c";
            default -> "#909399";
        };
    }

    private SysUser findUserByUsername(String username) {
        return sysUserRepository.findByUsername(username).orElse(null);
    }

    private String mapRoleCode(String code) {
        if ("ADMIN".equals(code)) return "admin";
        if ("REVIEWER".equals(code)) return "reviewer";
        return "editor";
    }

    private void addNodeAndChildren(DataEntry node, List<DataEntry> result) {
        result.add(node);
        List<DataEntry> children = entryRepository.findByVersionIdAndParentId(node.getVersionId(), node.getId());
        children.sort(Comparator.<DataEntry, Integer>comparing(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));
        for (DataEntry child : children) {
            addNodeAndChildren(child, result);
        }
    }

    public List<Long> collectL3AndDescendantIds(Long l3Id) {
        DataEntry l3 = entryRepository.findById(l3Id).orElse(null);
        if (l3 == null) return List.of();
        List<Long> ids = new ArrayList<>();
        ids.add(l3Id);
        collectDescendantIds(l3.getVersionId(), l3Id, ids);
        return ids;
    }

    private String cleanToText(String html) {
        Pattern cardPattern = Pattern.compile("<(?:span|div)[^>]+class=\"(?:image-card|img-card)\"[^>]*>", Pattern.DOTALL);
        Pattern urlPattern = Pattern.compile("data-url=\"([^\"]+)\"");
        Matcher cm = cardPattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (cm.find()) {
            String openTag = cm.group();
            String url = null;
            Matcher um = urlPattern.matcher(openTag);
            if (um.find()) url = um.group(1);
            if (url == null) continue;
            int depth = 1, pos = cm.end(), contentEnd = -1;
            String tagName = openTag.startsWith("<div") ? "div" : "span";
            while (pos < html.length() && depth > 0) {
                int no = html.indexOf("<" + tagName, pos);
                int nc = html.indexOf("</" + tagName + ">", pos);
                if (nc < 0) break;
                if (no >= 0 && no < nc) { depth++; pos = no + 1; }
                else { depth--; if (depth == 0) contentEnd = nc + tagName.length() + 3; pos = nc + tagName.length() + 3; }
            }
            String replacement = "<div style='text-align:center;margin:8px 0;'><img src='" + url + "' style='max-width:100%;max-height:400px;border:1px solid #e2e8f0;border-radius:4px;' /></div>";
            if (contentEnd > 0) {
                cm.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        cm.appendTail(sb);
        return sb.toString().replaceAll("<[^>]+>", "");
    }
}
