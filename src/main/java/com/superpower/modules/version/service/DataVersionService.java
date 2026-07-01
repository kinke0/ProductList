package com.superpower.modules.version.service;

import com.superpower.common.BusinessException;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.category.repository.BaseProductL1Repository;
import com.superpower.modules.category.repository.BaseProductL2Repository;
import com.superpower.modules.category.repository.BaseProductRepository;
import com.superpower.modules.category.service.CategoryService;
import com.superpower.modules.category.service.ProductService;
import com.superpower.modules.customtab.entity.CustomTab;
import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.customtab.repository.CustomTabRepository;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.document.repository.DocGenRecordRepository;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.repository.ImageResourceRepository;
import com.superpower.modules.option.repository.DataOptionRepository;
import com.superpower.modules.option.service.DataOptionService;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class DataVersionService {

    private final DataVersionRepository versionRepository;
    private final DataEntryRepository entryRepository;
    private final ImageResourceRepository imageResourceRepository;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final DataOptionService optionService;
    private final DataOptionRepository optionRepository;
    private final BaseCategoryRepository categoryRepository;
    private final BaseDomainRepository domainRepository;
    private final BaseProductRepository productRepository;
    private final BaseProductL1Repository productL1Repository;
    private final BaseProductL2Repository productL2Repository;
    private final CustomTabRepository customTabRepository;
    private final CustomTabEntryRepository customTabEntryRepository;
    private final DocGenRecordRepository docGenRecordRepository;
    private final PlatformTransactionManager txManager;

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataVersionService.class);

    private final ExecutorService versionOpExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "version-op");
        t.setDaemon(true);
        return t;
    });

    private volatile VersionProgress progress = null;

    public DataVersionService(DataVersionRepository versionRepository,
                              DataEntryRepository entryRepository,
                              ImageResourceRepository imageResourceRepository,
                              CategoryService categoryService,
                              ProductService productService,
                              DataOptionService optionService,
                              DataOptionRepository optionRepository,
                              BaseCategoryRepository categoryRepository,
                              BaseDomainRepository domainRepository,
                              BaseProductRepository productRepository,
                              BaseProductL1Repository productL1Repository,
                              BaseProductL2Repository productL2Repository,
                              CustomTabRepository customTabRepository,
                              CustomTabEntryRepository customTabEntryRepository,
                              DocGenRecordRepository docGenRecordRepository,
                              PlatformTransactionManager txManager) {
        this.versionRepository = versionRepository;
        this.entryRepository = entryRepository;
        this.imageResourceRepository = imageResourceRepository;
        this.categoryService = categoryService;
        this.productService = productService;
        this.optionService = optionService;
        this.optionRepository = optionRepository;
        this.categoryRepository = categoryRepository;
        this.domainRepository = domainRepository;
        this.productRepository = productRepository;
        this.productL1Repository = productL1Repository;
        this.productL2Repository = productL2Repository;
        this.customTabRepository = customTabRepository;
        this.customTabEntryRepository = customTabEntryRepository;
        this.docGenRecordRepository = docGenRecordRepository;
        this.txManager = txManager;
    }

    public List<DataVersion> findAllReleased() {
        return versionRepository.findAllReleased();
    }

    public List<DataVersion> findAll() {
        return versionRepository.findAll();
    }

    public DataVersion findById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("版本不存在"));
    }

    public VersionProgress getProgress() {
        return progress;
    }

    private void initProgress(String operation, List<String> stepNames) {
        List<StepStatus> steps = new ArrayList<>();
        for (int i = 0; i < stepNames.size(); i++) {
            steps.add(new StepStatus(i + 1, stepNames.get(i), "PENDING", null, 0));
        }
        progress = new VersionProgress(operation, steps, "RUNNING", null);
    }

    private void updateStep(int stepIndex, String status, String message, int count) {
        if (progress != null && stepIndex < progress.steps.size()) {
            StepStatus s = progress.steps.get(stepIndex);
            s.status = status;
            s.message = message;
            s.count = count;
        }
    }

    private void completeProgress(String result) {
        if (progress != null) {
            progress.status = "COMPLETED";
            progress.result = result;
        }
    }

    private void failProgress(String error) {
        if (progress != null) {
            progress.status = "FAILED";
            progress.result = error;
        }
    }

    public DataVersion createVersion() {
        if (versionRepository.existsByStatus("draft")) {
            throw new BusinessException("已存在编辑中的版本，请先封板发布后再创建新版本");
        }

        String newVersionNo = "1.0";
        DataVersion latest = versionRepository.findTopByOrderByCreatedAtDesc().orElse(null);
        if (latest != null) {
            String lastNo = latest.getVersionNo();
            String[] parts = lastNo.split("\\.");
            int minor = Integer.parseInt(parts[1]) + 1;
            newVersionNo = parts[0] + "." + minor;
        }

        DataVersion version = new DataVersion();
        version.setVersionNo(newVersionNo);
        version.setStatus("draft");
        final DataVersion toSave = version;
        version = new TransactionTemplate(txManager).execute(status -> versionRepository.save(toSave));

        if (latest == null || !"released".equals(latest.getStatus())) {
            return version;
        }

        List<String> stepNames = Arrays.asList(
                "复制清单数据",
                "复制业务分类",
                "复制产品分类",
                "复制基础选项",
                "复制图片资源",
                "复制自定义清单",
                "更新图片URL引用",
                "更新分类ID引用"
        );
        initProgress("CREATE", stepNames);

        final Long sourceVersionId = latest.getId();
        final Long targetVersionId = version.getId();
        final DataVersion finalVersion = version;

        versionOpExecutor.submit(() -> {
            try {
                doCreateVersionSteps(sourceVersionId, targetVersionId);
                completeProgress("版本 " + finalVersion.getVersionNo() + " 创建成功");
            } catch (Exception e) {
                log.error("创建版本失败", e);
                failProgress("创建版本失败: " + e.getMessage());
            }
        });

        return version;
    }

    private void doCreateVersionSteps(Long sourceVersionId, Long targetVersionId) {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        tx.executeWithoutResult(status -> {
            // Step 1: 复制清单数据
            updateStep(0, "RUNNING", "正在复制清单数据...", 0);
            List<DataEntry> entries = entryRepository.findByVersionId(sourceVersionId);
            HashMap<Long, Long> idMap = new HashMap<>();
            HashMap<Long, Long> oldParentMap = new HashMap<>();
            int cnt = 0;
            for (DataEntry entry : entries) {
                oldParentMap.put(entry.getId(), entry.getParentId());
                DataEntry copy = entry.cloneWithoutId();
                copy.setVersionId(targetVersionId);
                copy.setParentId(entry.getParentId());
                DataEntry saved = entryRepository.save(copy);
                idMap.put(entry.getId(), saved.getId());
                cnt++;
                if (cnt % 100 == 0) {
                    updateStep(0, "RUNNING", "正在复制清单数据...", cnt);
                }
            }
            for (Map.Entry<Long, Long> e : idMap.entrySet()) {
                Long oldParent = oldParentMap.get(e.getKey());
                if (oldParent != null) {
                    Long newParent = idMap.get(oldParent);
                    if (newParent != null) {
                        DataEntry en = entryRepository.findById(e.getValue()).orElse(null);
                        if (en != null) {
                            en.setParentId(newParent);
                            entryRepository.save(en);
                        }
                    }
                }
            }
            updateStep(0, "COMPLETED", "已复制 " + cnt + " 条清单数据", cnt);

            // Step 2: 复制业务分类
            updateStep(1, "RUNNING", "正在复制业务分类...", 0);
            Map<String, Map<Long, Long>> catResult = categoryService.copyFromVersion(sourceVersionId, targetVersionId);
            Map<Long, Long> catIdMap = catResult.get("catIdMap");
            Map<Long, Long> domIdMap = catResult.get("domIdMap");
            updateStep(1, "COMPLETED", "已复制 " + catIdMap.size() + " 个分类, " + domIdMap.size() + " 个业务域", catIdMap.size() + domIdMap.size());

            // Step 3: 复制产品分类
            updateStep(2, "RUNNING", "正在复制产品分类...", 0);
            Map<String, Map<Long, Long>> prodResult = productService.copyFromVersion(sourceVersionId, targetVersionId);
            updateStep(2, "COMPLETED", "已复制 " + prodResult.get("l1IdMap").size() + " 个L1, " + prodResult.get("l2IdMap").size() + " 个L2", prodResult.get("l1IdMap").size() + prodResult.get("l2IdMap").size());

            // Step 4: 复制基础选项
            updateStep(3, "RUNNING", "正在复制基础选项...", 0);
            optionService.copyOptions(sourceVersionId, targetVersionId);
            updateStep(3, "COMPLETED", "已复制基础选项（解决方案/功能状态/系统类型/应用角色）", 0);

            // Step 5: 复制图片资源（物理目录整体复制 + 数据库记录）
            updateStep(4, "RUNNING", "正在复制图片资源...", 0);

            Path srcImgDir = Paths.get(storagePath, String.valueOf(sourceVersionId));
            Path tgtImgDir = Paths.get(storagePath, String.valueOf(targetVersionId));
            if (Files.exists(srcImgDir)) {
                try {
                    Files.walkFileTree(srcImgDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                            Path targetPath = tgtImgDir.resolve(srcImgDir.relativize(dir));
                            Files.createDirectories(targetPath);
                            return FileVisitResult.CONTINUE;
                        }
                        @Override
                        public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
                            Path targetPath = tgtImgDir.resolve(srcImgDir.relativize(file));
                            Files.copy(file, targetPath, StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.warn("复制图片目录失败: {} -> {}: {}", srcImgDir, tgtImgDir, e.getMessage());
                }
            }

            List<ImageResource> images = imageResourceRepository.findByVersionIdOrderByCreatedAtDesc(sourceVersionId);
            HashMap<Long, Long> imgIdMap = new HashMap<>();
            int imgCount = 0;
            for (ImageResource img : images) {
                ImageResource copy = new ImageResource();
                copy.setFilename(img.getFilename());
                copy.setStoredName(img.getStoredName());
                copy.setCategory(img.getCategory());
                copy.setDomain(img.getDomain());
                copy.setProduct(img.getProduct());
                copy.setSize(img.getSize());
                copy.setMimeType(img.getMimeType());
                copy.setUploadedBy(img.getUploadedBy());
                copy.setVersionId(targetVersionId);
                copy.setWidth(img.getWidth());
                copy.setHeight(img.getHeight());

                if (img.getProductId() != null && idMap.containsKey(img.getProductId())) {
                    copy.setProductId(idMap.get(img.getProductId()));
                }

                String newUrlPfx = "/api/images/file/" + targetVersionId + "/";
                String newReqPfx = "/api/requirements/file/" + targetVersionId + "/";

                if (img.getUrl() != null) {
                    String url = img.getUrl();
                    url = url.replace("/api/images/file/" + sourceVersionId + "/", newUrlPfx);
                    url = url.replace("/api/requirements/file/" + sourceVersionId + "/", newReqPfx);
                    copy.setUrl(url);
                } else {
                    copy.setUrl(null);
                }

                if (img.getPath() != null) {
                    String normalizedPath = img.getPath().replace("\\", "/");
                    String oldPathPrefix = storagePath.replace("\\", "/");
                    if (!oldPathPrefix.endsWith("/")) oldPathPrefix += "/";
                    oldPathPrefix += sourceVersionId + "/";
                    if (normalizedPath.startsWith(oldPathPrefix)) {
                        String relative = normalizedPath.substring(oldPathPrefix.length());
                        copy.setPath(Paths.get(storagePath, String.valueOf(targetVersionId), relative).toString());
                    } else {
                        copy.setPath(img.getPath());
                    }
                }

                copy = imageResourceRepository.save(copy);
                imgIdMap.put(img.getId(), copy.getId());
                imgCount++;
                if (imgCount % 50 == 0) {
                    updateStep(4, "RUNNING", "正在复制图片资源...", imgCount);
                }
            }
            updateStep(4, "COMPLETED", "已复制 " + imgCount + " 个图片资源", imgCount);

            // Step 6: 复制自定义清单
            updateStep(5, "RUNNING", "正在复制自定义清单...", 0);
            List<CustomTab> oldTabs = customTabRepository.findByVersionIdOrderByCreatedAtAsc(sourceVersionId);
            int tabEntryCount = 0;
            for (CustomTab oldTab : oldTabs) {
                CustomTab newTab = new CustomTab();
                newTab.setName(oldTab.getName());
                newTab.setVersionId(targetVersionId);
                newTab.setUserId(oldTab.getUserId());
                newTab = customTabRepository.save(newTab);

                List<CustomTabEntry> oldEntries = customTabEntryRepository.findByCustomTabId(oldTab.getId());
                for (CustomTabEntry ote : oldEntries) {
                    Long newEntryId = idMap.get(ote.getEntryId());
                    if (newEntryId != null) {
                        CustomTabEntry newCte = new CustomTabEntry();
                        newCte.setCustomTabId(newTab.getId());
                        newCte.setEntryId(newEntryId);
                        customTabEntryRepository.save(newCte);
                        tabEntryCount++;
                    }
                }
            }
            updateStep(5, "COMPLETED", "已复制自定义清单及 " + tabEntryCount + " 条关联", tabEntryCount);

            // Step 7: 更新图片URL引用 + data-id映射 + 分类ID引用
            updateStep(6, "RUNNING", "正在更新图片URL和分类ID引用...", 0);
            String newUrlPrefix = "/api/images/file/" + targetVersionId + "/";
            List<DataVersion> allVersions = versionRepository.findAll();
            List<String> oldUrlPrefixes = new java.util.ArrayList<>();
            for (DataVersion v : allVersions) {
                if (!v.getId().equals(targetVersionId)) {
                    oldUrlPrefixes.add("/api/images/file/" + v.getId() + "/");
                }
            }
            java.util.regex.Pattern dataIdPattern = java.util.regex.Pattern.compile("data-id=\"(\\d+)\"");
            Map<Long, Long> productIdMap = prodResult.getOrDefault("productIdMap", java.util.Collections.emptyMap());
            List<DataEntry> newEntries = entryRepository.findByVersionId(targetVersionId);
            int urlUpdated = 0;
            int idUpdated = 0;
            int dataIdUpdated = 0;
            for (DataEntry entry : newEntries) {
                boolean changed = false;

                String desc = entry.getColFeatureDesc();
                if (desc != null && desc.contains("/api/images/file/")) {
                    for (String oldPfx : oldUrlPrefixes) {
                        if (desc.contains(oldPfx)) {
                            desc = desc.replace(oldPfx, newUrlPrefix);
                        }
                    }
                    if (imgIdMap != null && !imgIdMap.isEmpty()) {
                        java.util.regex.Matcher m = dataIdPattern.matcher(desc);
                        StringBuffer sb = new StringBuffer();
                        boolean found = false;
                        while (m.find()) {
                            Long oldId = Long.valueOf(m.group(1));
                            Long newId = imgIdMap.get(oldId);
                            if (newId != null) {
                                m.appendReplacement(sb, "data-id=\"" + newId + "\"");
                                found = true;
                            }
                        }
                        m.appendTail(sb);
                        if (found) {
                            desc = sb.toString();
                            dataIdUpdated++;
                        }
                    }
                    entry.setColFeatureDesc(desc);
                    changed = true;
                }
                String cp1 = entry.getColControlPointImg1();
                if (cp1 != null && cp1.contains("/api/images/file/")) {
                    for (String oldPfx : oldUrlPrefixes) {
                        cp1 = cp1.replace(oldPfx, newUrlPrefix);
                    }
                    entry.setColControlPointImg1(cp1);
                    changed = true;
                }
                String cp2 = entry.getColControlPointImg2();
                if (cp2 != null && cp2.contains("/api/images/file/")) {
                    for (String oldPfx : oldUrlPrefixes) {
                        cp2 = cp2.replace(oldPfx, newUrlPrefix);
                    }
                    entry.setColControlPointImg2(cp2);
                    changed = true;
                }
                String cp3 = entry.getColControlPointImg3();
                if (cp3 != null && cp3.contains("/api/images/file/")) {
                    for (String oldPfx : oldUrlPrefixes) {
                        cp3 = cp3.replace(oldPfx, newUrlPrefix);
                    }
                    entry.setColControlPointImg3(cp3);
                    changed = true;
                }
                String cpDoc = entry.getColControlPointDoc();
                if (cpDoc != null && cpDoc.contains("/api/images/file/")) {
                    for (String oldPfx : oldUrlPrefixes) {
                        cpDoc = cpDoc.replace(oldPfx, newUrlPrefix);
                    }
                    entry.setColControlPointDoc(cpDoc);
                    changed = true;
                }
                if (changed) {
                    urlUpdated++;
                }

                boolean catChanged = false;
                if (entry.getCategoryId() != null && catIdMap.containsKey(entry.getCategoryId())) {
                    entry.setCategoryId(catIdMap.get(entry.getCategoryId()));
                    catChanged = true;
                }
                if (entry.getDomainId() != null && domIdMap.containsKey(entry.getDomainId())) {
                    entry.setDomainId(domIdMap.get(entry.getDomainId()));
                    catChanged = true;
                }
                if (entry.getProductId() != null && productIdMap.containsKey(entry.getProductId())) {
                    entry.setProductId(productIdMap.get(entry.getProductId()));
                    catChanged = true;
                }
                if (catChanged) {
                    idUpdated++;
                }

                if (changed || catChanged) {
                    entryRepository.save(entry);
                }
            }
            updateStep(6, "COMPLETED", "已更新 " + urlUpdated + " 条图片URL, " + dataIdUpdated + " 条data-id, " + idUpdated + " 条分类ID", urlUpdated + idUpdated);

            // Step 8: (已合并到 Step 7，标记为完成)
            updateStep(7, "COMPLETED", "已在步骤7中同步完成", 0);
        });
    }

    @Transactional
    public DataVersion getOrCreateInitialVersion() {
        List<DataVersion> all = versionRepository.findAll();
        if (!all.isEmpty()) {
            return all.get(0);
        }
        return createVersionDirect();
    }

    @Transactional
    public DataVersion createVersionDirect() {
        if (versionRepository.existsByStatus("draft")) {
            throw new BusinessException("已存在编辑中的版本，请先封板发布后再创建新版本");
        }

        String newVersionNo = "1.0";
        DataVersion latest = versionRepository.findTopByOrderByCreatedAtDesc().orElse(null);
        if (latest != null) {
            String lastNo = latest.getVersionNo();
            String[] parts = lastNo.split("\\.");
            int minor = Integer.parseInt(parts[1]) + 1;
            newVersionNo = parts[0] + "." + minor;
        }

        DataVersion version = new DataVersion();
        version.setVersionNo(newVersionNo);
        version.setStatus("draft");
        return versionRepository.save(version);
    }

    public void deleteVersion(Long versionId) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        final String versionNo = tx.execute(status -> {
            DataVersion version = versionRepository.findById(versionId)
                    .orElseThrow(() -> new BusinessException("版本不存在"));
            long totalCount = versionRepository.count();
            if (totalCount <= 1) {
                throw new BusinessException("唯一版本不允许删除");
            }
            return version.getVersionNo();
        });

        List<String> stepNames = Arrays.asList(
                "删除自定义清单项",
                "删除自定义清单",
                "删除清单数据",
                "删除业务分类",
                "删除产品分类",
                "删除基础选项",
                "删除图片资源",
                "删除文档生成记录",
                "删除版本记录"
        );
        initProgress("DELETE", stepNames);

        final Long vid = versionId;

        versionOpExecutor.submit(() -> {
            try {
                doDeleteVersionSteps(vid);
                completeProgress("版本 " + versionNo + " 已删除");
            } catch (Exception e) {
                log.error("删除版本失败", e);
                failProgress("删除版本失败: " + e.getMessage());
            }
        });
    }

    private void doDeleteVersionSteps(Long versionId) {
        TransactionTemplate tx = new TransactionTemplate(txManager);

        List<String> imagePaths = tx.execute(status -> {
            // Step 1: 删除自定义清单项
            updateStep(0, "RUNNING", "正在删除自定义清单项...", 0);
            List<CustomTab> tabs = customTabRepository.findByVersionIdOrderByCreatedAtAsc(versionId);
            List<Long> tabIds = new ArrayList<>();
            for (CustomTab tab : tabs) {
                tabIds.add(tab.getId());
            }
            if (!tabIds.isEmpty()) {
                customTabEntryRepository.deleteByCustomTabIdIn(tabIds);
            }
            updateStep(0, "COMPLETED", "已删除 " + tabIds.size() + " 个清单的关联项", tabIds.size());

            // Step 2: 删除自定义清单
            updateStep(1, "RUNNING", "正在删除自定义清单...", 0);
            customTabRepository.deleteByVersionId(versionId);
            updateStep(1, "COMPLETED", "已删除自定义清单", tabIds.size());

            // Step 3: 删除清单数据
            updateStep(2, "RUNNING", "正在删除清单数据...", 0);
            long entryCount = entryRepository.countByVersionId(versionId);
            entryRepository.deleteByVersionId(versionId);
            updateStep(2, "COMPLETED", "已删除 " + entryCount + " 条清单数据", (int) entryCount);

            // Step 4: 删除业务分类
            updateStep(3, "RUNNING", "正在删除业务分类...", 0);
            long bpCount = productRepository.countByVersionId(versionId);
            long bdCount = domainRepository.countByVersionId(versionId);
            long bcCount = categoryRepository.countByVersionId(versionId);
            productRepository.deleteByVersionId(versionId);
            domainRepository.deleteByVersionId(versionId);
            categoryRepository.deleteByVersionId(versionId);
            updateStep(3, "COMPLETED", "已删除 " + bcCount + " 个分类, " + bdCount + " 个业务域, " + bpCount + " 个产品", (int) (bcCount + bdCount + bpCount));

            // Step 5: 删除产品分类
            updateStep(4, "RUNNING", "正在删除产品分类...", 0);
            long l2Count = productL2Repository.countByVersionId(versionId);
            long l1Count = productL1Repository.countByVersionId(versionId);
            productL2Repository.deleteByVersionId(versionId);
            productL1Repository.deleteByVersionId(versionId);
            updateStep(4, "COMPLETED", "已删除 " + l1Count + " 个L1, " + l2Count + " 个L2", (int) (l1Count + l2Count));

            // Step 6: 删除基础选项
            updateStep(5, "RUNNING", "正在删除基础选项...", 0);
            optionRepository.deleteByVersionId(versionId);
            updateStep(5, "COMPLETED", "已删除基础选项（解决方案/功能状态/系统类型/应用角色）", 0);

            // Step 7: 删除图片资源（数据库记录）
            updateStep(6, "RUNNING", "正在删除图片资源...", 0);
            List<ImageResource> images = imageResourceRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
            List<String> paths = new ArrayList<>();
            for (ImageResource img : images) {
                if (img.getPath() != null) {
                    paths.add(img.getPath());
                }
            }
            imageResourceRepository.deleteByVersionId(versionId);
            updateStep(6, "COMPLETED", "已删除 " + images.size() + " 个图片资源", images.size());

            // Step 8: 删除文档生成记录
            updateStep(7, "RUNNING", "正在删除文档生成记录...", 0);
            docGenRecordRepository.deleteByVersionId(versionId);
            updateStep(7, "COMPLETED", "已删除文档生成记录", 0);

            // Step 9: 删除版本记录
            updateStep(8, "RUNNING", "正在删除版本记录...", 0);
            versionRepository.deleteById(versionId);
            updateStep(8, "COMPLETED", "已删除版本记录", 1);

            return paths;
        });

        // 事务提交后，安全删除物理文件
        if (imagePaths != null) {
            for (String path : imagePaths) {
                try {
                    Files.deleteIfExists(Paths.get(path));
                } catch (IOException e) {
                    log.warn("删除图片文件失败: {}", path);
                }
            }
            try {
                Path imgDir = Paths.get(storagePath, String.valueOf(versionId));
                if (Files.exists(imgDir)) {
                    Files.walk(imgDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException e) {
                                    log.warn("删除目录失败: {}", p);
                                }
                            });
                }
            } catch (IOException e) {
                log.warn("删除图片目录失败: {}", e.getMessage());
            }
        }
    }

    @Transactional
    public DataVersion releaseVersion(Long versionId, Long userId) {
        DataVersion version = findById(versionId);
        if (!"draft".equals(version.getStatus())) {
            throw new BusinessException("版本状态不正确");
        }
        if (version.getRollbackCount() != null && version.getRollbackCount() > 0) {
            String baseNo = version.getVersionNo();
            int dotCount = 0;
            for (char c : baseNo.toCharArray()) {
                if (c == '.') dotCount++;
            }
            if (dotCount < 2) {
                version.setVersionNo(baseNo + ".1");
            } else {
                int lastDot = baseNo.lastIndexOf('.');
                String prefix = baseNo.substring(0, lastDot + 1);
                String suffix = baseNo.substring(lastDot + 1);
                int patch = Integer.parseInt(suffix) + 1;
                version.setVersionNo(prefix + patch);
            }
        }
        version.setStatus("released");
        version.setReleasedAt(LocalDateTime.now());
        version.setReleasedBy(userId);
        return versionRepository.save(version);
    }

    @Transactional
    public DataVersion rollbackVersion(Long versionId) {
        DataVersion version = findById(versionId);
        if (!"released".equals(version.getStatus())) {
            throw new BusinessException("只能退回已发布的版本");
        }
        if (versionRepository.existsByStatus("draft")) {
            throw new BusinessException("已存在编辑中的版本，请先处理后再退回");
        }
        version.setStatus("draft");
        version.setRollbackCount(version.getRollbackCount() != null ? version.getRollbackCount() + 1 : 1);
        version.setReleasedAt(null);
        version.setReleasedBy(null);
        return versionRepository.save(version);
    }

    public static class VersionProgress {
        public String operation;
        public List<StepStatus> steps;
        public String status;
        public String result;

        public VersionProgress(String operation, List<StepStatus> steps, String status, String result) {
            this.operation = operation;
            this.steps = steps;
            this.status = status;
            this.result = result;
        }
    }

    public static class StepStatus {
        public int step;
        public String name;
        public String status;
        public String message;
        public int count;

        public StepStatus(int step, String name, String status, String message, int count) {
            this.step = step;
            this.name = name;
            this.status = status;
            this.message = message;
            this.count = count;
        }
    }
}
