package com.superpower.modules.category.service;

import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.data.dto.TreeNodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CategoryService {

    private final BaseCategoryRepository categoryRepository;
    private final BaseDomainRepository domainRepository;

    public CategoryService(BaseCategoryRepository categoryRepository,
                           BaseDomainRepository domainRepository) {
        this.categoryRepository = categoryRepository;
        this.domainRepository = domainRepository;
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
                child.setIsLeaf(true);
                children.add(child);
            }
            node.setChildren(children);
            result.add(node);
        }
        return result;
    }

    @Transactional
    public void copyFromVersion(Long sourceVersionId, Long targetVersionId) {
        Map<Long, Long> catIdMap = new HashMap<>();
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
                domainRepository.save(dom);
            }
        }
    }
}
