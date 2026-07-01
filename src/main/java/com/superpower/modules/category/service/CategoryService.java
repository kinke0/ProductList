package com.superpower.modules.category.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.entity.BaseProduct;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.category.repository.BaseProductRepository;
import com.superpower.modules.data.dto.TreeNodeDTO;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CategoryService {

    private final BaseCategoryRepository categoryRepository;
    private final BaseDomainRepository domainRepository;
    private final BaseProductRepository productRepository;
    private final DataEntryRepository dataEntryRepository;

    public CategoryService(BaseCategoryRepository categoryRepository,
                           BaseDomainRepository domainRepository,
                           BaseProductRepository productRepository,
                           DataEntryRepository dataEntryRepository) {
        this.categoryRepository = categoryRepository;
        this.domainRepository = domainRepository;
        this.productRepository = productRepository;
        this.dataEntryRepository = dataEntryRepository;
    }

    public List<TreeNodeDTO> getTree(Long versionId) {
        List<BaseCategory> categories = categoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
        List<TreeNodeDTO> result = new ArrayList<>();
        for (BaseCategory cat : categories) {
            TreeNodeDTO node = new TreeNodeDTO();
            node.setId(cat.getId());
            node.setParentId(null);
            node.setLevel(1);
            node.setLabel(cat.getName());
            node.setSortOrder(cat.getSortOrder());
            node.setIsLeaf(false);

            List<BaseDomain> domains = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(versionId, cat.getId());
            List<TreeNodeDTO> children = new ArrayList<>();
            for (BaseDomain dom : domains) {
                TreeNodeDTO child = new TreeNodeDTO();
                child.setId(dom.getId());
                child.setParentId(cat.getId());
                child.setLevel(2);
                child.setLabel(dom.getName());
                child.setSortOrder(dom.getSortOrder());
                child.setIsLeaf(false);

                List<BaseProduct> products = productRepository.findByVersionIdAndDomainIdOrderBySortOrderAsc(versionId, dom.getId());
                List<TreeNodeDTO> grandChildren = new ArrayList<>();
                for (BaseProduct prod : products) {
                    TreeNodeDTO grandChild = new TreeNodeDTO();
                    grandChild.setId(prod.getId());
                    grandChild.setParentId(dom.getId());
                    grandChild.setLevel(3);
                    grandChild.setLabel(prod.getName());
                    grandChild.setSortOrder(prod.getSortOrder());
                    grandChild.setIsLeaf(true);
                    grandChildren.add(grandChild);
                }
                child.setChildren(grandChildren);
                children.add(child);
            }
            node.setChildren(children);
            result.add(node);
        }
        return result;
    }

    @Transactional
    public void updateSortOrders(Long versionId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            String type = (String) item.get("type");
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            if ("category".equals(type)) {
                BaseCategory cat = categoryRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("分类不存在"));
                cat.setSortOrder(sortOrder);
                categoryRepository.save(cat);
            } else if ("domain".equals(type)) {
                BaseDomain dom = domainRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("业务域不存在"));
                dom.setSortOrder(sortOrder);
                domainRepository.save(dom);
            } else if ("product".equals(type)) {
                BaseProduct prod = productRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("产品分类不存在"));
                prod.setSortOrder(sortOrder);
                productRepository.save(prod);
            }
        }
    }

    public BaseCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("分类不存在: " + id));
    }

    public BaseDomain getDomainById(Long id) {
        return domainRepository.findById(id)
            .orElseThrow(() -> new BusinessException("业务域不存在: " + id));
    }

    @Transactional
    public BaseCategory createCategory(Long versionId, String name) {
        long count = categoryRepository.findByVersionIdOrderBySortOrderAsc(versionId).size();
        BaseCategory cat = new BaseCategory();
        cat.setVersionId(versionId);
        cat.setName(name);
        cat.setSortOrder((int) count);
        BaseCategory saved = categoryRepository.save(cat);
        DataEntry entry = new DataEntry();
        entry.setVersionId(versionId);
        entry.setLevel(1);
        entry.setColBizCategory(name);
        entry.setColProductSystem(name);
        entry.setCategoryId(saved.getId());
        entry.setIsLeaf(true);
        int maxSort = dataEntryRepository.findByVersionIdAndLevel(versionId, 1)
            .stream().mapToInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0).max().orElse(-1);
        entry.setSortOrder(maxSort + 1);
        dataEntryRepository.save(entry);
        return saved;
    }

    @Transactional
    public BaseCategory updateCategory(Long id, String name) {
        BaseCategory cat = getCategoryById(id);
        String oldName = cat.getName();
        cat.setName(name);
        categoryRepository.save(cat);
        List<DataEntry> entries = dataEntryRepository.findByVersionIdAndCategoryId(cat.getVersionId(), cat.getId());
        for (DataEntry e : entries) {
            e.setColBizCategory(name);
            if (name.equals(e.getColProductSystem()) || oldName.equals(e.getColProductSystem())) {
                e.setColProductSystem(name);
            }
            dataEntryRepository.save(e);
        }
        return cat;
    }

    @Transactional
    public void deleteCategory(Long id) {
        BaseCategory cat = getCategoryById(id);
        long domainCount = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(cat.getVersionId(), cat.getId()).size();
        if (domainCount > 0) {
            throw new BusinessException("该分类下存在业务域，不可删除");
        }
        // 删除该分类的 L1 分隔行（基于 categoryId，而非名称）
        dataEntryRepository.findByVersionIdAndCategoryIdAndLevel(cat.getVersionId(), cat.getId(), 1)
            .forEach(dataEntryRepository::delete);
        categoryRepository.delete(cat);
    }

    @Transactional
    public BaseDomain createDomain(Long versionId, Long categoryId, String name) {
        BaseCategory cat = getCategoryById(categoryId);
        long count = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(versionId, categoryId).size();
        BaseDomain dom = new BaseDomain();
        dom.setVersionId(versionId);
        dom.setCategoryId(categoryId);
        dom.setName(name);
        dom.setSortOrder((int) count);
        BaseDomain saved = domainRepository.save(dom);
        DataEntry entry = new DataEntry();
        entry.setVersionId(versionId);
        entry.setLevel(2);
        entry.setColBizCategory(cat.getName());
        entry.setColBizDomain(name);
        entry.setColProductSystem(name);
        entry.setCategoryId(categoryId);
        entry.setDomainId(saved.getId());
        entry.setIsLeaf(true);
        Long l1EntryId = findL1EntryIdByVersionIdAndCategoryId(versionId, categoryId);
        if (l1EntryId != null) {
            entry.setParentId(l1EntryId);
            DataEntry parent = dataEntryRepository.findById(l1EntryId).orElse(null);
            if (parent != null && parent.getIsLeaf()) {
                parent.setIsLeaf(false);
                dataEntryRepository.save(parent);
            }
            int maxSort = dataEntryRepository.findByVersionIdAndParentId(versionId, l1EntryId)
                .stream().mapToInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0).max().orElse(-1);
            entry.setSortOrder(maxSort + 1);
        } else {
            entry.setSortOrder(0);
        }
        dataEntryRepository.save(entry);
        return saved;
    }

    @Transactional
    public BaseDomain updateDomain(Long id, String name) {
        BaseDomain dom = getDomainById(id);
        String oldName = dom.getName();
        dom.setName(name);
        domainRepository.save(dom);
        List<DataEntry> entries = dataEntryRepository.findByVersionIdAndDomainId(dom.getVersionId(), dom.getId());
        for (DataEntry e : entries) {
            e.setColBizDomain(name);
            if (oldName.equals(e.getColProductSystem())) {
                e.setColProductSystem(name);
            }
            dataEntryRepository.save(e);
        }
        return dom;
    }

    @Transactional
    public void deleteDomain(Long id) {
        BaseDomain dom = getDomainById(id);
        List<DataEntry> l2Entries = dataEntryRepository.findByVersionIdAndDomainIdAndLevel(dom.getVersionId(), dom.getId(), 2);
        for (DataEntry e : l2Entries) {
            if (dataEntryRepository.findByVersionIdAndParentId(dom.getVersionId(), e.getId()).size() > 0) {
                throw new BusinessException("业务域\"" + dom.getName() + "\"下存在子级条目，不可删除");
            }
        }
        l2Entries.forEach(dataEntryRepository::delete);
        domainRepository.delete(dom);
    }

    private Long findL1EntryIdByVersionIdAndCategoryId(Long versionId, Long categoryId) {
        List<DataEntry> l1Entries = dataEntryRepository.findByVersionIdAndCategoryIdAndLevel(versionId, categoryId, 1);
        if (!l1Entries.isEmpty()) {
            return l1Entries.get(0).getId();
        }
        return null;
    }

    @Transactional
    public Map<String, Map<Long, Long>> copyFromVersion(Long sourceVersionId, Long targetVersionId) {
        Map<Long, Long> catIdMap = new HashMap<>();
        Map<Long, Long> domIdMap = new HashMap<>();
        List<BaseCategory> srcCategories = categoryRepository.findByVersionIdOrderBySortOrderAsc(sourceVersionId);
        for (BaseCategory src : srcCategories) {
            BaseCategory cat = new BaseCategory();
            cat.setVersionId(targetVersionId);
            cat.setName(src.getName());
            cat.setSortOrder(src.getSortOrder());
            cat = categoryRepository.save(cat);
            catIdMap.put(src.getId(), cat.getId());

                List<BaseDomain> domains = domainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(sourceVersionId, src.getId());
            for (BaseDomain srcDom : domains) {
                BaseDomain dom = new BaseDomain();
                dom.setVersionId(targetVersionId);
                dom.setCategoryId(cat.getId());
                dom.setName(srcDom.getName());
                dom.setSortOrder(srcDom.getSortOrder());
                dom = domainRepository.save(dom);
                domIdMap.put(srcDom.getId(), dom.getId());
            }
        }
        Map<String, Map<Long, Long>> result = new HashMap<>();
        result.put("catIdMap", catIdMap);
        result.put("domIdMap", domIdMap);
        return result;
    }
}
