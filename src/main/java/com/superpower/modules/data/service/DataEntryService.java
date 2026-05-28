package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataEntryService {

    private final DataEntryRepository entryRepository;
    private final DataVersionRepository dataVersionRepository;
    private final CustomTabEntryRepository customTabEntryRepository;

    public DataEntryService(DataEntryRepository entryRepository, DataVersionRepository dataVersionRepository,
                            CustomTabEntryRepository customTabEntryRepository) {
        this.entryRepository = entryRepository;
        this.dataVersionRepository = dataVersionRepository;
        this.customTabEntryRepository = customTabEntryRepository;
    }

    public List<TreeNodeDTO> getTree(Long versionId, String name, String status, String productManager,
                                     String solution, String versionTag) {
        List<DataEntry> entries = entryRepository.findByVersionIdAndLevelWithFilter(
                versionId, 1, name, status, productManager, solution, versionTag);
        return entries.stream().map(e -> buildTree(e, versionId)).toList();
    }

    private TreeNodeDTO buildTree(DataEntry entry, Long versionId) {
        TreeNodeDTO node = new TreeNodeDTO();
        node.setId(entry.getId());
        node.setParentId(entry.getParentId());
        node.setLevel(entry.getLevel());
        node.setLabel(entry.getColProductSystem() != null ? entry.getColProductSystem() : entry.getColBizCategory());
        node.setSortOrder(entry.getSortOrder());

        if (entry.getLevel() < 2) {
            node.setIsLeaf(false);
            List<DataEntry> children = entryRepository.findByVersionIdAndParentIdOrderBySortOrder(versionId, entry.getId());
            if (!children.isEmpty()) {
                node.setChildren(children.stream().map(c -> buildTree(c, versionId)).toList());
            }
        } else {
            node.setIsLeaf(true);
        }
        return node;
    }

    public List<DataEntry> getChildren(Long versionId, Long parentId, String name, String status,
                                      String productManager, String solution, String versionTag) {
        return entryRepository.findByVersionIdAndParentIdWithFilter(
                versionId, parentId, name, status, productManager, solution, versionTag);
    }

    public DataEntry getById(Long id) {
        return entryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据条目不存在"));
    }

    @Transactional
    public List<DataEntry> query(Long versionId, Long customTabId, String name, String status, String productManager,
                                 String solution, String versionDivision, String bizCategory, String bizDomain) {
        boolean hasFilter = (name != null && !name.isEmpty()) || (status != null && !status.isEmpty())
                || (productManager != null && !productManager.isEmpty()) || (solution != null && !solution.isEmpty())
                || (versionDivision != null && !versionDivision.isEmpty())
                || (bizCategory != null && !bizCategory.isEmpty()) || (bizDomain != null && !bizDomain.isEmpty());

        if (customTabId != null) {
            List<DataEntry> entries;
            if (hasFilter) {
                entries = entryRepository.queryEntries(versionId, customTabId, name, status, productManager,
                        solution, versionDivision, bizCategory, bizDomain);
            } else {
                entries = entryRepository.findEntriesByTab(versionId, customTabId);
            }
            return reorderByCustomTabSort(customTabId, entries);
        }

        if (hasFilter) {
            return entryRepository.queryEntries(versionId, null, name, status, productManager,
                    solution, versionDivision, bizCategory, bizDomain);
        }

        if (bizCategory != null || bizDomain != null) {
            return entryRepository.findEntriesByDomain(versionId, bizCategory, bizDomain);
        }

        return entryRepository.findAllEntries(versionId);
    }

    @Transactional
    public DataEntry create(DataEntryDTO dto) {
        ensureVersionEditable(dto.getVersionId());

        DataEntry entry = new DataEntry();
        copyFields(entry, dto);
        entry.setVersionId(dto.getVersionId());
        entry.setParentId(dto.getParentId());
        entry.setLevel(dto.getLevel());
        entry.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
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
        DataEntry saved = entryRepository.save(entry);

        if (entry.getLevel() != null && entry.getLevel() <= 2) {
            cascadeLabelUpdate(entry, oldBizCategory, oldBizDomain);
        }

        return saved;
    }

    private void cascadeLabelUpdate(DataEntry entry, String oldBizCategory, String oldBizDomain) {
        String newCategory = entry.getColBizCategory();
        String newDomain = entry.getColBizDomain();

        if (entry.getLevel() == 1 && newCategory != null && !newCategory.equals(oldBizCategory)) {
            List<DataEntry> descendants = collectDescendants(entry.getId(), entry.getVersionId());
            for (DataEntry d : descendants) {
                d.setColBizCategory(newCategory);
                entryRepository.save(d);
            }
        }

        if (entry.getLevel() == 2 && newDomain != null && !newDomain.equals(oldBizDomain)) {
            List<DataEntry> descendants = collectDescendants(entry.getId(), entry.getVersionId());
            for (DataEntry d : descendants) {
                d.setColBizDomain(newDomain);
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
}
