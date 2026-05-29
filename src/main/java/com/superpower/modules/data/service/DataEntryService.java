package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.ExcelImportResult;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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

    public void batchDelete(List<Long> ids) {
        List<Long> allIds = new ArrayList<>();
        for (Long id : ids) {
            DataEntry entry = entryRepository.findById(id).orElse(null);
            if (entry == null) continue;
            allIds.add(id);
            collectDescendantIds(entry.getVersionId(), id, allIds);
        }
        for (Long id : allIds) {
            entryRepository.deleteById(id);
        }
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
                    processRow(rd, headerMap, versionId, result, nameToId);
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
                processRow(rd, headerMap, versionId, result, nameToId);
            }
        } catch (Exception e) {
            result.getErrors().add("解析Excel文件失败: " + e.getMessage());
        }
        return result;
    }

    private void processRow(RowData rd, Map<String, Integer> headerMap, Long versionId,
                            ExcelImportResult result, Map<String, Long> nameToId) {
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
            entry.setColBizCategory(bizCategory);
            entry.setColBizDomain(bizDomain);
            entry.setColProductSystem(productName);
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
            setStr(headerMap, row, entry, "其他解决方案标记", DataEntry::setColOtherSolutionTag);
            setStr(headerMap, row, entry, "文档维护人员", DataEntry::setColDocMaintainer);
            setStr(headerMap, row, entry, "产品经理", DataEntry::setColProductManager);
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

    private String getCellString(Row row, int colIdx) {
        if (colIdx < 0) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        cell.setCellType(CellType.STRING);
        String val = cell.getStringCellValue();
        return val != null ? val.trim() : null;
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
}
