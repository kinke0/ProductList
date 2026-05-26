package com.superpower.modules.data.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.data.dto.DataEntryDTO;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataEntryService {

    private final DataEntryRepository entryRepository;
    private final DataVersionRepository dataVersionRepository;

    public DataEntryService(DataEntryRepository entryRepository, DataVersionRepository dataVersionRepository) {
        this.entryRepository = entryRepository;
        this.dataVersionRepository = dataVersionRepository;
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

    public List<DataEntry> query(Long versionId, Long customTabId, String name, String status, String productManager,
                                 String solution, String versionDivision, String bizCategory, String bizDomain) {
        return entryRepository.queryEntries(versionId, customTabId, name, status, productManager,
                solution, versionDivision, bizCategory, bizDomain);
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
        copyFields(entry, dto);
        return entryRepository.save(entry);
    }

    @Transactional
    public void delete(Long id) {
        DataEntry entry = getById(id);
        ensureVersionEditable(entry.getVersionId());
        List<DataEntry> children = entryRepository.findByVersionIdAndParentId(entry.getVersionId(), id);
        if (!children.isEmpty()) {
            throw new BusinessException("该节点下有子节点，无法删除");
        }
        entryRepository.deleteById(id);
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
}
