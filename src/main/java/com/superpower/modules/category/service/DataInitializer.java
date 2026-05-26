package com.superpower.modules.category.service;

import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.service.DataVersionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BaseCategoryRepository categoryRepository;
    private final BaseDomainRepository domainRepository;
    private final DataVersionService versionService;

    public DataInitializer(BaseCategoryRepository categoryRepository,
                           BaseDomainRepository domainRepository,
                           DataVersionService versionService) {
        this.categoryRepository = categoryRepository;
        this.domainRepository = domainRepository;
        this.versionService = versionService;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) return;

        DataVersion version = versionService.getOrCreateInitialVersion();

        Map<String, Integer> categories = new LinkedHashMap<>();
        categories.put("1. 数智底座-数据", 1);
        categories.put("3. 数智底座-人工智能", 3);
        categories.put("4. 数智底座-技术", 4);
        categories.put("5. 智慧医疗", 5);
        categories.put("6. 智慧服务", 6);
        categories.put("8. 智慧科研", 8);
        categories.put("9. 智慧医联", 9);

        Map<String, String[]> domains = new LinkedHashMap<>();
        domains.put("1. 数智底座-数据", new String[]{"1.1 大数据平台", "1.2 临床数据应用", "1.3 运营数据应用"});
        domains.put("3. 数智底座-人工智能", new String[]{"3.1 AI智能平台"});
        domains.put("4. 数智底座-技术", new String[]{"4.1 技术开发", "4.2 系统管理", "4.3 信息集成"});
        domains.put("5. 智慧医疗", new String[]{"5.1 门诊诊疗业务", "5.2 急诊诊疗业务", "5.3 住院诊疗业务", "5.4 辅助诊断业务", "5.5 治疗业务", "5.6 医疗保障业务", "5.7 病案管理业务", "5.8 护理管理业务", "5.9 医疗质量业务", "5.10 医保管理业务"});
        domains.put("6. 智慧服务", new String[]{"6.1 服务资源优化", "6.2 互联网服务", "6.3 医疗智能终端"});
        domains.put("8. 智慧科研", new String[]{"8.2 专病数据库", "8.3 科研数据应用"});
        domains.put("9. 智慧医联", new String[]{"9.1 区域医疗服务协同", "9.2 便民惠民服务协同", "9.3 基层医疗卫生综合管理"});

        for (Map.Entry<String, Integer> catEntry : categories.entrySet()) {
            BaseCategory cat = new BaseCategory();
            cat.setVersionId(version.getId());
            cat.setName(catEntry.getKey());
            cat.setSortOrder(catEntry.getValue());
            cat = categoryRepository.save(cat);

            String[] domList = domains.get(catEntry.getKey());
            if (domList != null) {
                for (String domName : domList) {
                    BaseDomain dom = new BaseDomain();
                    dom.setVersionId(version.getId());
                    dom.setCategoryId(cat.getId());
                    dom.setName(domName);
                    dom.setSortOrder(extractSortOrder(domName));
                    domainRepository.save(dom);
                }
            }
        }
    }

    private int extractSortOrder(String name) {
        if (name == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)").matcher(name.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
