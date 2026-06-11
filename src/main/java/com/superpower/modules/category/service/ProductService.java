package com.superpower.modules.category.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.category.entity.BaseProduct;
import com.superpower.modules.category.entity.BaseProductL1;
import com.superpower.modules.category.entity.BaseProductL2;
import com.superpower.modules.category.repository.BaseProductRepository;
import com.superpower.modules.category.repository.BaseProductL1Repository;
import com.superpower.modules.category.repository.BaseProductL2Repository;
import com.superpower.modules.data.dto.ExcelImportResult;
import com.superpower.modules.option.entity.DataOption;
import com.superpower.modules.option.repository.DataOptionRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class ProductService {

    private final BaseProductRepository productRepository;
    private final BaseProductL1Repository productL1Repository;
    private final BaseProductL2Repository productL2Repository;
    private final DataOptionRepository dataOptionRepository;

    public ProductService(BaseProductRepository productRepository,
                         BaseProductL1Repository productL1Repository,
                         BaseProductL2Repository productL2Repository,
                         DataOptionRepository dataOptionRepository) {
        this.productRepository = productRepository;
        this.productL1Repository = productL1Repository;
        this.productL2Repository = productL2Repository;
        this.dataOptionRepository = dataOptionRepository;
    }

    // L1 操作
    public List<BaseProductL1> getL1List(Long versionId) {
        return productL1Repository.findByVersionIdOrderBySortOrderAsc(versionId);
    }

    public BaseProductL1 getL1ById(Long id) {
        return productL1Repository.findById(id)
            .orElseThrow(() -> new BusinessException("统计分类不存在: " + id));
    }

    @Transactional
    public BaseProductL1 createL1(Long versionId, String name) {
        long count = productL1Repository.findByVersionIdOrderBySortOrderAsc(versionId).size();
        BaseProductL1 l1 = new BaseProductL1();
        l1.setVersionId(versionId);
        l1.setName(name);
        l1.setSortOrder((int) count);
        return productL1Repository.save(l1);
    }

    @Transactional
    public BaseProductL1 updateL1(Long id, String name) {
        BaseProductL1 l1 = getL1ById(id);
        l1.setName(name);
        return productL1Repository.save(l1);
    }

    @Transactional
    public void deleteL1(Long id) {
        BaseProductL1 l1 = getL1ById(id);
        List<BaseProductL2> l2List = productL2Repository.findByVersionIdAndL1IdOrderBySortOrderAsc(l1.getVersionId(), id);
        if (!l2List.isEmpty()) {
            throw new BusinessException("该统计分类下存在核心业务方向，不可删除");
        }
        productL1Repository.delete(l1);
    }

    // L2 操作
    public List<BaseProductL2> getL2List(Long versionId, Long l1Id) {
        return productL2Repository.findByVersionIdAndL1IdOrderBySortOrderAsc(versionId, l1Id);
    }

    public BaseProductL2 getL2ById(Long id) {
        return productL2Repository.findById(id)
            .orElseThrow(() -> new BusinessException("核心业务方向不存在: " + id));
    }

    @Transactional
    public BaseProductL2 createL2(Long versionId, Long l1Id, String name) {
        BaseProductL1 l1 = getL1ById(l1Id);
        long count = productL2Repository.findByVersionIdAndL1IdOrderBySortOrderAsc(versionId, l1Id).size();
        BaseProductL2 l2 = new BaseProductL2();
        l2.setVersionId(versionId);
        l2.setL1Id(l1Id);
        l2.setName(name);
        l2.setSortOrder((int) count);
        return productL2Repository.save(l2);
    }

    @Transactional
    public BaseProductL2 updateL2(Long id, String name) {
        BaseProductL2 l2 = getL2ById(id);
        l2.setName(name);
        return productL2Repository.save(l2);
    }

    @Transactional
    public void deleteL2(Long id) {
        BaseProductL2 l2 = getL2ById(id);
        List<BaseProduct> products = productRepository.findByVersionIdAndDomainIdOrderBySortOrderAsc(l2.getVersionId(), id);
        if (!products.isEmpty()) {
            throw new BusinessException("该核心业务方向下存在核心业务产品，不可删除");
        }
        productL2Repository.delete(l2);
    }

    // L3 操作
    public List<BaseProduct> getProductList(Long versionId, Long l2Id) {
        return productRepository.findByVersionIdAndDomainIdOrderBySortOrderAsc(versionId, l2Id);
    }

    public BaseProduct getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new BusinessException("核心业务产品不存在: " + id));
    }

    @Transactional
    public BaseProduct createProduct(Long versionId, Long l2Id, String name) {
        BaseProductL2 l2 = getL2ById(l2Id);
        long count = productRepository.findByVersionIdAndDomainIdOrderBySortOrderAsc(versionId, l2Id).size();
        BaseProduct product = new BaseProduct();
        product.setVersionId(versionId);
        product.setDomainId(l2Id);
        product.setL1Id(l2.getL1Id());
        product.setL2Id(l2Id);
        product.setName(name);
        product.setSortOrder((int) count);
        return productRepository.save(product);
    }

    @Transactional
    public BaseProduct updateProduct(Long id, String name) {
        BaseProduct product = getProductById(id);
        product.setName(name);
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        BaseProduct product = getProductById(id);
        productRepository.delete(product);
    }

    // 排序操作
    @Transactional
    public void updateL1SortOrders(Long versionId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            BaseProductL1 l1 = getL1ById(id);
            l1.setSortOrder(sortOrder);
            productL1Repository.save(l1);
        }
    }

    @Transactional
    public void updateL2SortOrders(Long versionId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            BaseProductL2 l2 = getL2ById(id);
            l2.setSortOrder(sortOrder);
            productL2Repository.save(l2);
        }
    }

    @Transactional
    public void updateProductSortOrders(Long versionId, List<Map<String, Object>> sortList) {
        for (Map<String, Object> item : sortList) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            BaseProduct product = getProductById(id);
            product.setSortOrder(sortOrder);
            productRepository.save(product);
        }
    }

    @Transactional
    public Map<String, Map<Long, Long>> copyFromVersion(Long sourceVersionId, Long targetVersionId) {
        Map<Long, Long> l1IdMap = new HashMap<>();
        Map<Long, Long> l2IdMap = new HashMap<>();
        Map<Long, Long> productIdMap = new HashMap<>();

        List<BaseProductL1> srcL1List = productL1Repository.findByVersionIdOrderBySortOrderAsc(sourceVersionId);
        for (BaseProductL1 src : srcL1List) {
            BaseProductL1 l1 = new BaseProductL1();
            l1.setVersionId(targetVersionId);
            l1.setName(src.getName());
            l1.setSortOrder(src.getSortOrder());
            l1 = productL1Repository.save(l1);
            l1IdMap.put(src.getId(), l1.getId());
        }

        List<BaseProductL2> srcL2List = productL2Repository.findByVersionId(sourceVersionId);
        for (BaseProductL2 src : srcL2List) {
            BaseProductL2 l2 = new BaseProductL2();
            l2.setVersionId(targetVersionId);
            l2.setL1Id(l1IdMap.get(src.getL1Id()));
            l2.setName(src.getName());
            l2.setSortOrder(src.getSortOrder());
            l2 = productL2Repository.save(l2);
            l2IdMap.put(src.getId(), l2.getId());
        }

        List<BaseProduct> srcProducts = productRepository.findByVersionId(sourceVersionId);
        for (BaseProduct src : srcProducts) {
            BaseProduct product = new BaseProduct();
            product.setVersionId(targetVersionId);
            product.setDomainId(l2IdMap.get(src.getDomainId()));
            product.setL1Id(l1IdMap.get(src.getL1Id()));
            product.setL2Id(l2IdMap.get(src.getL2Id()));
            product.setName(src.getName());
            product.setSortOrder(src.getSortOrder());
            product = productRepository.save(product);
            productIdMap.put(src.getId(), product.getId());
        }

        Map<String, Map<Long, Long>> result = new HashMap<>();
        result.put("l1IdMap", l1IdMap);
        result.put("l2IdMap", l2IdMap);
        result.put("productIdMap", productIdMap);
        return result;
    }

    // 导入Excel
    @Transactional
    public ExcelImportResult importFromExcel(MultipartFile file, Long versionId) {
        ExcelImportResult result = new ExcelImportResult();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) {
                result.getErrors().add("Excel文件中没有数据行");
                return result;
            }
            result.setTotalRows(lastRow);

            // 第一遍：收集L1(统计分类)、L2(核心业务产品=第3列)、系统归类(第4列)
            LinkedHashSet<String> l1Set = new LinkedHashSet<>();
            LinkedHashSet<String> l2Set = new LinkedHashSet<>();
            LinkedHashSet<String> systemTypeSet = new LinkedHashSet<>();

            String currentL1 = null;
            String currentL3 = null;
            for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                String l1Val = getCellString(row, 0);
                String l3Val = getCellString(row, 2);
                String l4Val = getCellString(row, 3);

                if (l1Val != null && !l1Val.isEmpty()) currentL1 = l1Val;
                if (l3Val != null && !l3Val.isEmpty()) currentL3 = l3Val;

                if (currentL1 != null && !l1Set.contains(currentL1)) l1Set.add(currentL1);
                if (currentL1 != null && currentL3 != null) {
                    String l2Key = currentL1 + "||" + currentL3;
                    if (!l2Set.contains(l2Key)) l2Set.add(l2Key);
                }
                if (l4Val != null && !l4Val.isEmpty() && !systemTypeSet.contains(l4Val)) {
                    systemTypeSet.add(l4Val);
                }
            }

            // 加载现有L1/L2到缓存
            Map<String, BaseProductL1> l1Cache = new HashMap<>();
            Map<String, BaseProductL2> l2Cache = new HashMap<>();

            for (BaseProductL1 l1 : productL1Repository.findByVersionIdOrderBySortOrderAsc(versionId)) {
                l1Cache.put(l1.getName(), l1);
            }
            for (BaseProductL2 l2 : productL2Repository.findByVersionId(versionId)) {
                l2Cache.put(l2.getL1Id() + ":" + l2.getName(), l2);
            }

            int l1SortOrder = l1Cache.size();
            int l2SortOrder = l2Cache.size();

            // 创建L1
            Map<String, BaseProductL1> l1NameToEntity = new HashMap<>();
            for (String l1Name : l1Set) {
                BaseProductL1 l1 = l1Cache.get(l1Name);
                if (l1 == null) {
                    l1 = new BaseProductL1();
                    l1.setVersionId(versionId);
                    l1.setName(l1Name);
                    l1.setSortOrder(l1SortOrder++);
                    l1 = productL1Repository.save(l1);
                    result.setSuccessRows(result.getSuccessRows() + 1);
                }
                l1NameToEntity.put(l1Name, l1);
            }

            // 创建L2（核心业务产品）
            for (String l2Key : l2Set) {
                String[] parts = l2Key.split("\\|\\|", 2);
                String l1Name = parts[0];
                String l2Name = parts[1];
                BaseProductL1 l1 = l1NameToEntity.get(l1Name);
                String cacheKey = l1.getId() + ":" + l2Name;
                BaseProductL2 l2 = l2Cache.get(cacheKey);
                if (l2 == null) {
                    l2 = new BaseProductL2();
                    l2.setVersionId(versionId);
                    l2.setL1Id(l1.getId());
                    l2.setName(l2Name);
                    l2.setSortOrder(l2SortOrder++);
                    l2 = productL2Repository.save(l2);
                    l2Cache.put(cacheKey, l2);
                    result.setSuccessRows(result.getSuccessRows() + 1);
                }
            }

            // 导入系统归类到sys_option表（type=systemType），智能合并
            Set<String> existingSystemTypes = new HashSet<>();
            for (DataOption opt : dataOptionRepository.findByTypeAndVersionIdOrderBySortOrder("systemType", versionId)) {
                existingSystemTypes.add(opt.getValue());
            }
            int systemTypeSortOrder = existingSystemTypes.size();
            for (String systemType : systemTypeSet) {
                if (!existingSystemTypes.contains(systemType)) {
                    DataOption opt = new DataOption();
                    opt.setType("systemType");
                    opt.setVersionId(versionId);
                    opt.setValue(systemType);
                    opt.setSortOrder(systemTypeSortOrder++);
                    dataOptionRepository.save(opt);
                    result.setSuccessRows(result.getSuccessRows() + 1);
                }
            }

        } catch (IOException e) {
            result.getErrors().add("解析Excel文件失败: " + e.getMessage());
        }

        return result;
    }

    private String getCellString(Row row, int cellIdx) {
        Cell cell = row.getCell(cellIdx);
        if (cell == null) return null;
        return cell.toString().trim();
    }
}
