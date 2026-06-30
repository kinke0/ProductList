package com.superpower.modules.image.service;

import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.common.BusinessException;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.image.dto.ImageDirectoryNode;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.dto.MigrationResult;
import com.superpower.modules.image.dto.MigrationTaskProgress;
import com.superpower.modules.image.repository.ImageResourceRepository;
import com.superpower.modules.requirement.entity.ReqItem;
import com.superpower.modules.requirement.repository.ReqItemRepository;
import com.superpower.modules.version.entity.DataVersion;
import com.superpower.modules.version.repository.DataVersionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ImageResourceService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final ImageResourceRepository imageResourceRepository;
    private final DataEntryRepository dataEntryRepository;
    private final BaseCategoryRepository baseCategoryRepository;
    private final BaseDomainRepository baseDomainRepository;
    private final ReqItemRepository reqItemRepository;
    private final DataVersionRepository dataVersionRepository;

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    private final ConcurrentHashMap<String, MigrationTaskProgress> taskStore = new ConcurrentHashMap<>();

    @Lazy
    private final ImageResourceService self;

    public ImageResourceService(ImageResourceRepository imageResourceRepository,
                                DataEntryRepository dataEntryRepository,
                                BaseCategoryRepository baseCategoryRepository,
                                BaseDomainRepository baseDomainRepository,
                                ReqItemRepository reqItemRepository,
                                DataVersionRepository dataVersionRepository,
                                @Lazy ImageResourceService self) {
        this.imageResourceRepository = imageResourceRepository;
        this.dataEntryRepository = dataEntryRepository;
        this.baseCategoryRepository = baseCategoryRepository;
        this.baseDomainRepository = baseDomainRepository;
        this.reqItemRepository = reqItemRepository;
        this.dataVersionRepository = dataVersionRepository;
        this.self = self;
    }

    @Transactional
    public ImageResource upload(MultipartFile file, String category, String domain, String product,
                                Long versionId, String username, String displayName) {
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("只允许上传jpg/png/gif/webp格式的图片");
        }

        product = resolveL3Product(product, category, domain, versionId);

        String originalFilename = file.getOriginalFilename();
        String ext = getExtension(originalFilename, contentType);
        String effectiveName = (displayName != null && !displayName.isEmpty()) ? displayName : originalFilename;
        String sanitizedBase = sanitizePath(effectiveName);
        if (sanitizedBase.isEmpty()) sanitizedBase = UUID.randomUUID().toString();
        String storedName = sanitizedBase + (sanitizedBase.endsWith("." + ext) ? "" : "." + ext);

        String subPath = buildSubPath(category, domain, product);
        String basePath;
        String urlPrefix;
        if (category != null && category.startsWith("需求")) {
            basePath = storagePath.replace("images", "requirements");
            urlPrefix = "/api/requirements/file/";
        } else {
            basePath = storagePath;
            urlPrefix = "/api/images/file/";
        }

        if (versionId != null) {
            List<ImageResource> existing = imageResourceRepository.findByVersionIdAndCategoryAndDomainAndProductOrderByCreatedAtDesc(
                    versionId, category, domain, product);
            for (ImageResource ex : existing) {
                if (storedName.equals(ex.getStoredName())) {
                    throw new BusinessException("当前版本该目录下已存在同名文件: " + storedName);
                }
            }
        }

        String versionDir = versionId != null ? String.valueOf(versionId) : "0";
        Path dirPath = Paths.get(basePath, versionDir, subPath);
        try {
            Files.createDirectories(dirPath);
            int waitRetry = 0;
            while (!Files.exists(dirPath) && waitRetry < 10) {
                Thread.sleep(50);
                waitRetry++;
            }
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            throw new BusinessException("创建目录失败: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("创建目录被中断");
        }

        Path filePath = dirPath.resolve(storedName);
        if (Files.exists(filePath)) {
            throw new BusinessException("同目录下已存在同名文件: " + storedName);
        }
        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败: " + e.getMessage());
        }

        String urlPath = urlPrefix + versionDir + "/" + subPath + "/" + storedName;

        ImageResource image = new ImageResource();
        String filenameToSave = (displayName != null && !displayName.isEmpty()) ? displayName : originalFilename;
        if (filenameToSave != null && !filenameToSave.isEmpty() && !filenameToSave.contains(".")) {
            filenameToSave = filenameToSave + "." + ext;
        }
        image.setFilename(filenameToSave);
        image.setStoredName(storedName);
        image.setPath(filePath.toString());
        image.setCategory(category);
        image.setDomain(domain);
        image.setProduct(product);
        image.setUrl(urlPath);
        image.setSize(file.getSize());
        image.setMimeType(contentType);
        image.setUploadedBy(username);
        image.setVersionId(versionId);

        if (product != null && versionId != null) {
            Long l3Id = findL3EntryId(versionId, product);
            image.setProductId(l3Id);
        }

        try {
            java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(filePath.toFile());
            if (bimg != null) {
                image.setWidth(bimg.getWidth());
                image.setHeight(bimg.getHeight());
            }
        } catch (Exception ignored) {}

        return imageResourceRepository.save(image);
    }

    public List<ImageResource> findAll(String category, String domain, String product, Long versionId, boolean includeReferenced) {
        return findAll(category, domain, product, null, versionId, includeReferenced);
    }

    public List<ImageResource> findAll(String category, String domain, String product, Long productId, Long versionId, boolean includeReferenced) {
        final String cat = stripCountSuffix(category);
        final String dom = stripCountSuffix(domain);
        final String prod = stripCountSuffix(product);
        List<ImageResource> direct = findDirect(cat, dom, prod, productId, versionId);

        if (includeReferenced && versionId != null && cat != null && dom != null) {
            List<DataEntry> matchingEntries = dataEntryRepository.findByVersionIdAndColBizCategoryAndColBizDomain(
                    versionId, cat, dom);
            if (prod != null) {
                Map<Long, DataEntry> entryMap = new HashMap<>();
                for (DataEntry e : dataEntryRepository.findByVersionId(versionId)) {
                    entryMap.put(e.getId(), e);
                }
                final String fProd = prod;
                matchingEntries = matchingEntries.stream()
                        .filter(e -> {
                            DataEntry l3 = findAncestorAtLevel(e, entryMap, 3);
                            return l3 != null && fProd.equals(l3.getColProductSystem());
                        })
                        .toList();
            }
            Set<Long> directIds = direct.stream().map(ImageResource::getId).collect(Collectors.toSet());
            List<ImageResource> referenced = findReferencedImages(matchingEntries, directIds);
            direct.addAll(referenced);
        }

        return direct;
    }

    private List<ImageResource> findDirect(String category, String domain, String product, Long productId, Long versionId) {
        if (versionId != null && productId != null) {
            return imageResourceRepository.findByVersionIdAndProductIdOrderByCreatedAtDesc(versionId, productId);
        }
        if (versionId != null && category != null && domain != null && product != null) {
            return imageResourceRepository.findByVersionIdAndCategoryAndDomainAndProductOrderByCreatedAtDesc(versionId, category, domain, product);
        }
        if (versionId != null && category != null && domain != null) {
            return imageResourceRepository.findByVersionIdAndCategoryAndDomainOrderByCreatedAtDesc(versionId, category, domain);
        }
        if (versionId != null && category != null) {
            return imageResourceRepository.findByVersionIdAndCategoryOrderByCreatedAtDesc(versionId, category);
        }
        if (versionId != null) {
            return imageResourceRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
        }
        if (category != null && domain != null && product != null) {
            return imageResourceRepository.findByCategoryAndDomainAndProductOrderByCreatedAtDesc(category, domain, product);
        }
        if (category != null && domain != null) {
            return imageResourceRepository.findByCategoryAndDomainOrderByCreatedAtDesc(category, domain);
        }
        if (category != null) {
            return imageResourceRepository.findByCategoryOrderByCreatedAtDesc(category);
        }
        return imageResourceRepository.findAll();
    }

    private List<ImageResource> findReferencedImages(List<DataEntry> entries, Set<Long> excludeIds) {
        List<String> descs = entries.stream()
                .map(DataEntry::getColFeatureDesc)
                .filter(d -> d != null && !d.isEmpty())
                .toList();
        if (descs.isEmpty()) return new ArrayList<>();

        Long versionId = entries.get(0).getVersionId();
        List<ImageResource> allImages = imageResourceRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
        List<ImageResource> result = new ArrayList<>();
        for (ImageResource img : allImages) {
            if (excludeIds.contains(img.getId())) continue;
            String url = img.getUrl();
            if (url == null) continue;
            for (String desc : descs) {
                if (desc.contains(url)) {
                    result.add(img);
                    excludeIds.add(img.getId());
                    break;
                }
            }
        }
        return result;
    }

    @Transactional
    public void delete(Long id) {
        ImageResource image = imageResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("图片不存在"));

        imageResourceRepository.deleteById(id);

        try {
            Path filePath = Paths.get(image.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {}
    }

    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            try {
                delete(id);
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public ImageResource update(Long id, ImageResource body) {
        ImageResource image = imageResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("图片不存在"));
        String oldUrl = image.getUrl();
        String oldName = image.getFilename();
        String originalPath = image.getPath(); // ★ 改实体前记录原始物理路径，供 afterCommit 移动
        boolean nameChanged = body.getFilename() != null && !body.getFilename().equals(image.getFilename());

        // Phase 1: 纯计算 + 同名校验（不动物理文件，不触发 dirty）
        String newStoredName = null;
        String newUrl = null;
        if (nameChanged) {
            String oldStoredName = image.getStoredName();
            String ext = "";
            int dotIdx = oldStoredName.lastIndexOf('.');
            if (dotIdx > 0) ext = oldStoredName.substring(dotIdx);
            newStoredName = sanitizePath(body.getFilename().replaceAll("\\.[^.]+$", "")) + ext;

            if (!newStoredName.equals(oldStoredName)) {
                if (image.getVersionId() != null) {
                    List<ImageResource> existing = imageResourceRepository.findByVersionIdAndCategoryAndDomainAndProductOrderByCreatedAtDesc(
                            image.getVersionId(), image.getCategory(), image.getDomain(), image.getProduct());
                    for (ImageResource ex : existing) {
                        if (!ex.getId().equals(image.getId()) && newStoredName.equals(ex.getStoredName())) {
                            throw new BusinessException("当前目录下已存在同名文件: " + newStoredName);
                        }
                    }
                }
                newUrl = oldUrl;
                if (oldUrl != null && oldUrl.contains(oldStoredName)) {
                    newUrl = oldUrl.substring(0, oldUrl.lastIndexOf(oldStoredName)) + newStoredName;
                }
            } else {
                newStoredName = null;
            }
        }

        // Phase 2: 修改实体 + 保存（事务体精简，持锁最短，弱网下最抗 SQLITE_BUSY）
        if (nameChanged) {
            image.setFilename(body.getFilename());
        }
        if (newStoredName != null) {
            image.setStoredName(newStoredName);
            if (newUrl != null) image.setUrl(newUrl);
        }
        boolean locationChanged = false;
        if (body.getCategory() != null && !body.getCategory().equals(image.getCategory())) {
            image.setCategory(body.getCategory());
            locationChanged = true;
        }
        if (body.getDomain() != null && !body.getDomain().equals(image.getDomain())) {
            image.setDomain(body.getDomain());
            locationChanged = true;
        }
        if (body.getProduct() != null && !body.getProduct().equals(image.getProduct())) {
            image.setProduct(body.getProduct());
            locationChanged = true;
        }
        // 目录变更或改名后，重新计算 path/url（仅计算并 set，不移动文件）
        if (locationChanged || newStoredName != null) {
            recomputeImageLocation(image);
        }
        imageResourceRepository.saveAndFlush(image);

        // Phase 3: 引用同步（事务内，与 image_resource 原子一致；DB 操作可回滚，必须留在事务内）
        if (nameChanged && newStoredName != null) {
            syncImageNameInReferences(image.getId(), image.getUrl(), image.getFilename());
        }

        // Phase 4: afterCommit —— 仅文件移动（不可回滚的文件系统操作，必须等事务提交后再执行）
        final String finalPath = image.getPath();
        final boolean needMove = !originalPath.equals(finalPath);
        if (needMove) {
            final Long imageId = image.getId();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            Path src = Paths.get(originalPath);
                            Path dst = Paths.get(finalPath);
                            if (Files.exists(src)) {
                                if (dst.getParent() != null) Files.createDirectories(dst.getParent());
                                Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception e) {
                            log.error("afterCommit 物理文件移动失败 imageId={} src={} dst={}: {}", imageId, originalPath, finalPath, e.getMessage());
                        }
                    }
                });
            } else {
                try {
                    Path src = Paths.get(originalPath);
                    Path dst = Paths.get(finalPath);
                    if (Files.exists(src)) {
                        if (dst.getParent() != null) Files.createDirectories(dst.getParent());
                        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    log.error("文件移动失败（无事务上下文） imageId={} src={} dst={}: {}", image.getId(), originalPath, finalPath, e.getMessage());
                }
            }
        }

        return image;
    }

    private void syncImageNameInReferences(Long imageId, String newUrl, String newName) {
        if (imageId == null || newUrl == null || newName == null) return;
        String nameSafe = newName.replace("\"", "&quot;").replace("'", "&#39;");
        String idMarker = "data-id=\"" + imageId + "\"";
        List<DataEntry> entries = dataEntryRepository.findByColFeatureDescContaining(idMarker);
        List<DataEntry> toSave = new ArrayList<>();
        for (DataEntry e : entries) {
            String desc = e.getColFeatureDesc();
            if (desc == null || !desc.contains(idMarker)) continue;
            String updated = replaceImageCardAttrs(desc, idMarker, newUrl, newName, nameSafe);
            if (updated != null) {
                e.setColFeatureDesc(updated);
                toSave.add(e);
            }
        }
        if (!toSave.isEmpty()) dataEntryRepository.saveAll(toSave);
        List<ReqItem> reqItems = reqItemRepository.findByDescriptionContaining(idMarker);
        List<ReqItem> reqToSave = new ArrayList<>();
        for (ReqItem item : reqItems) {
            String desc = item.getDescription();
            if (desc == null || !desc.contains(idMarker)) continue;
            String updated = replaceImageCardAttrs(desc, idMarker, newUrl, newName, nameSafe);
            if (updated != null) {
                item.setDescription(updated);
                reqToSave.add(item);
            }
        }
        if (!reqToSave.isEmpty()) reqItemRepository.saveAll(reqToSave);
    }

    private String replaceImageCardAttrs(String desc, String idMarker, String newUrl, String newName, String nameSafe) {
        String original = desc;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "<span\\s+class=\"image-card\"[^>]*" + java.util.regex.Pattern.quote(idMarker) + "[^>]*>").matcher(desc);
        StringBuffer sb = new StringBuffer();
        boolean found = false;
        while (m.find()) {
            found = true;
            String tag = m.group();
            tag = tag.replaceAll("data-url=\"[^\"]*\"", "data-url=\"" + newUrl + "\"");
            tag = tag.replaceAll("data-filename=\"[^\"]*\"", "data-filename=\"" + nameSafe + "\"");
            tag = tag.replaceAll("title=\"[^\"]*\"", "title=\"" + nameSafe + "\"");
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(tag));
        }
        m.appendTail(sb);
        String result = sb.toString();
        result = result.replaceAll(
                "(<img\\s+[^>]*?src=\")([^\"]*?)(\"[^>]*?alt=\")([^\"]*?)(\")",
                "$1" + java.util.regex.Matcher.quoteReplacement(newUrl) + "$3" + java.util.regex.Matcher.quoteReplacement(nameSafe) + "$5");
        result = result.replaceAll(
                "(<span\\s+class=\"image-name\"[^>]*>)([^<]*?)(</span>)",
                "$1" + java.util.regex.Matcher.quoteReplacement(nameSafe) + "$3");
        return result.equals(original) ? null : result;
    }

    private String escapeRegex(String s) {
        return s.replace("$", "\\$").replace("(", "\\(").replace(")", "\\)")
                .replace("[", "\\[").replace("]", "\\]").replace("*", "\\*")
                .replace("+", "\\+").replace("?", "\\?").replace(".", "\\.");
    }

    @Transactional
    public ImageResource replaceFile(Long id, MultipartFile file) {
        ImageResource image = imageResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("图片不存在"));
        if (file.isEmpty()) throw new BusinessException("文件不能为空");
        if (file.getSize() > MAX_FILE_SIZE) throw new BusinessException("文件大小不能超过5MB");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException("只允许上传jpg/png/gif/webp格式的图片");
        }
        Path existingPath = Paths.get(image.getPath());
        try {
            Files.copy(file.getInputStream(), existingPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("文件替换失败: " + e.getMessage());
        }
        image.setSize(file.getSize());
        image.setMimeType(contentType);
        return imageResourceRepository.save(image);
    }

    public List<ImageDirectoryNode> getTree(Long versionId) {
        List<DataEntry> entries = versionId != null
            ? dataEntryRepository.findByVersionId(versionId)
            : dataEntryRepository.findAll();

        Map<String, String> catNames = new LinkedHashMap<>();
        Map<String, List<String>> catToDomains = new LinkedHashMap<>();
        Map<String, List<String>> domToProducts = new LinkedHashMap<>();

        if (versionId != null) {
            List<BaseCategory> baseCategories = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
            for (BaseCategory cat : baseCategories) {
                String catName = cat.getName();
                catNames.put(catName, catName);

                List<BaseDomain> domains = baseDomainRepository.findByVersionIdAndCategoryIdOrderBySortOrderAsc(versionId, cat.getId());
                for (BaseDomain dom : domains) {
                    String domName = dom.getName();
                    catToDomains.computeIfAbsent(catName, k -> new ArrayList<>());
                    String domListKey = catName + "||" + domName;
                    if (catToDomains.get(catName).stream().noneMatch(d -> d.equals(domName))) {
                        catToDomains.get(catName).add(domName);
                    }

                    List<DataEntry> level3Entries = dataEntryRepository.findByVersionIdAndDomainIdAndLevel(versionId, dom.getId(), 3);
                    if (level3Entries.isEmpty()) {
                        level3Entries = dataEntryRepository.findByVersionIdAndColBizDomainAndLevel(versionId, domName, 3);
                    }
                    for (DataEntry e : level3Entries) {
                        String prod = e.getColProductSystem();
                        if (prod != null && !prod.trim().isEmpty()) {
                            prod = prod.trim();
                            domToProducts.computeIfAbsent(domListKey, k -> new ArrayList<>());
                            if (!domToProducts.get(domListKey).contains(prod)) {
                                domToProducts.get(domListKey).add(prod);
                            }
                        }
                    }
                }
            }
        } else {
            Map<Long, DataEntry> entryMapForTree = new HashMap<>();
            for (DataEntry e : entries) entryMapForTree.put(e.getId(), e);
            for (DataEntry entry : entries) {
                String cat = entry.getColBizCategory();
                String dom = entry.getColBizDomain();
                if (cat == null || cat.isEmpty()) continue;
                cat = cat.trim();
                catNames.putIfAbsent(cat, cat);
                if (dom != null && !dom.trim().isEmpty()) {
                    final String domTrimmed = dom.trim();
                    catToDomains.computeIfAbsent(cat, k -> new ArrayList<>());
                    String domListKey = cat + "||" + domTrimmed;
                    if (catToDomains.get(cat).stream().noneMatch(d -> d.equals(domTrimmed))) {
                        catToDomains.get(cat).add(domTrimmed);
                    }
                    if (entry.getLevel() != null && entry.getLevel() == 3) {
                        String prod = entry.getColProductSystem();
                        if (prod != null && !prod.trim().isEmpty()) {
                            prod = prod.trim();
                            domToProducts.computeIfAbsent(domListKey, k -> new ArrayList<>());
                            if (!domToProducts.get(domListKey).contains(prod)) {
                                domToProducts.get(domListKey).add(prod);
                            }
                        }
                    }
                }
            }
        }

        List<String> orderedCategories = new ArrayList<>(catNames.keySet());

        List<ImageResource> allImages = versionId != null
            ? imageResourceRepository.findByVersionIdOrderByCreatedAtDesc(versionId)
            : imageResourceRepository.findAll();

        Map<String, Integer> prodCountMap = new HashMap<>();

        Map<String, List<ImageResource>> imgByCatDom = new HashMap<>();
        for (ImageResource img : allImages) {
            String imgCat = img.getCategory() != null ? img.getCategory() : "";
            String imgDom = img.getDomain() != null ? img.getDomain() : "";
            imgByCatDom.computeIfAbsent(imgCat + "||" + imgDom, k -> new ArrayList<>()).add(img);
        }

        for (String catName : orderedCategories) {
            List<String> domains = catToDomains.getOrDefault(catName, new ArrayList<>());
            for (String domName : domains) {
                String domKey = catName + "||" + domName;
                List<ImageResource> catDomImages = imgByCatDom.getOrDefault(domKey, Collections.emptyList());
                Map<String, Integer> directCountByProd = new HashMap<>();
                for (ImageResource img : catDomImages) {
                    String imgProd = img.getProduct() != null ? img.getProduct() : "";
                    directCountByProd.merge(imgProd, 1, Integer::sum);
                }
                List<String> products = domToProducts.getOrDefault(domKey, new ArrayList<>());
                for (String prodName : products) {
                    prodCountMap.put(domKey + "||" + prodName, directCountByProd.getOrDefault(prodName, 0));
                }
            }
        }

        Map<String, Set<String>> prodDirectImgIdSets = new HashMap<>();
        Set<String> allDirectImgIds = new HashSet<>();
        for (String catName : orderedCategories) {
            List<String> domains = catToDomains.getOrDefault(catName, new ArrayList<>());
            for (String domName : domains) {
                String domKey = catName + "||" + domName;
                List<ImageResource> catDomImages = imgByCatDom.getOrDefault(domKey, Collections.emptyList());
                List<String> products = domToProducts.getOrDefault(domKey, new ArrayList<>());
                for (String prodName : products) {
                    Set<String> ids = new HashSet<>();
                    for (ImageResource img : catDomImages) {
                        String imgProd = img.getProduct() != null ? img.getProduct() : "";
                        if (imgProd.equals(prodName)) {
                            String id = String.valueOf(img.getId());
                            ids.add(id);
                            allDirectImgIds.add(id);
                        }
                    }
                    prodDirectImgIdSets.put(domKey + "||" + prodName, ids);
                }
            }
        }

        Set<String> catDomProdKeys = prodDirectImgIdSets.keySet();

        Map<String, String> urlToImgId = new HashMap<>();
        for (ImageResource img : allImages) {
            if (img.getUrl() != null) {
                urlToImgId.put(img.getUrl(), String.valueOf(img.getId()));
            }
        }

        Map<Long, DataEntry> entryMap = new HashMap<>();
        for (DataEntry e : entries) entryMap.put(e.getId(), e);

        Map<Long, String> allEntryL3Product = new HashMap<>();
        for (DataEntry e : entries) {
            DataEntry l3 = findAncestorAtLevel(e, entryMap, 3);
            String l3Prod = (l3 != null && l3.getColProductSystem() != null) ? l3.getColProductSystem().trim() : null;
            allEntryL3Product.put(e.getId(), l3Prod);
        }

        Map<String, Set<String>> prodRefIds = new HashMap<>();
        for (String key : catDomProdKeys) {
            prodRefIds.put(key, new HashSet<>());
        }

        java.util.regex.Pattern urlExtractPattern = java.util.regex.Pattern.compile("/api/images/[^\"'<>\\]]+");

        for (DataEntry e : entries) {
            String desc = e.getColFeatureDesc();
            if (desc == null || desc.isEmpty() || !desc.contains("/api/images/")) continue;
            String eCat = e.getColBizCategory() != null ? e.getColBizCategory().trim() : "";
            String eDom = e.getColBizDomain() != null ? e.getColBizDomain().trim() : "";
            String domKey = eCat + "||" + eDom;
            String l3Prod = allEntryL3Product.get(e.getId());
            if (l3Prod == null) continue;
            String prodKey = domKey + "||" + l3Prod;
            if (!prodRefIds.containsKey(prodKey)) continue;
            Set<String> targetSet = prodRefIds.get(prodKey);
            java.util.regex.Matcher m = urlExtractPattern.matcher(desc);
            while (m.find()) {
                String url = m.group();
                String imgId = urlToImgId.get(url);
                if (imgId == null) continue;
                if (allDirectImgIds.contains(imgId)) continue;
                targetSet.add(imgId);
            }
        }

        for (String prodKey : catDomProdKeys) {
            Set<String> thisDirectIds = prodDirectImgIdSets.getOrDefault(prodKey, Collections.emptySet());
            Set<String> refs = prodRefIds.getOrDefault(prodKey, Collections.emptySet());
            int refCount = 0;
            for (String refId : refs) {
                if (!thisDirectIds.contains(refId)) {
                    refCount++;
                }
            }
            if (refCount > 0) {
                prodCountMap.merge(prodKey, refCount, Integer::sum);
            }
        }

        List<ImageDirectoryNode> roots = new ArrayList<>();
        for (String catName : orderedCategories) {
            ImageDirectoryNode catNode = new ImageDirectoryNode();
            List<String> domains = catToDomains.getOrDefault(catName, new ArrayList<>());

            List<ImageDirectoryNode> domainNodes = new ArrayList<>();
            int catTotal = 0;
            for (String domName : domains) {
                String domKey = catName + "||" + domName;
                ImageDirectoryNode domNode = new ImageDirectoryNode();

                List<ImageDirectoryNode> prodNodes = new ArrayList<>();
                List<String> products = domToProducts.getOrDefault(domKey, new ArrayList<>());
                int domTotal = 0;
                for (String prodName : products) {
                    String prodKey = domKey + "||" + prodName;
                    int prodCount = prodCountMap.getOrDefault(prodKey, 0);
                    ImageDirectoryNode prodNode = new ImageDirectoryNode();
                    prodNode.setLabel(prodName + " (" + prodCount + ")");
                    prodNode.setCount(prodCount);
                    prodNode.setChildren(null);
                    prodNodes.add(prodNode);
                    domTotal += prodCount;
                }
                domNode.setLabel(domName + " (" + domTotal + ")");
                domNode.setCount(domTotal);
                domNode.setChildren(prodNodes.isEmpty() ? null : prodNodes);
                domainNodes.add(domNode);
                catTotal += domTotal;
            }
            catNode.setLabel(catName + " (" + catTotal + ")");
            catNode.setCount(catTotal);
            catNode.setChildren(domainNodes.isEmpty() ? null : domainNodes);
            roots.add(catNode);
        }

        return roots;
    }

    private int fuzzyGetCount(Map<String, Integer> countMap, String key) {
        Integer count = countMap.get(key);
        if (count != null) return count;
        String keyNorm = key.replace(" ", "");
        for (Map.Entry<String, Integer> e : countMap.entrySet()) {
            if (e.getKey().replace(" ", "").equals(keyNorm)) {
                return e.getValue();
            }
        }
        return 0;
    }

    private DataEntry findAncestorAtLevel(DataEntry entry, Map<Long, DataEntry> entryMap, int targetLevel) {
        DataEntry current = entry;
        while (current != null && current.getLevel() != null && current.getLevel() > targetLevel) {
            Long parentId = current.getParentId();
            if (parentId == null) break;
            current = entryMap.get(parentId);
        }
        return (current != null && current.getLevel() != null && current.getLevel() == targetLevel) ? current : null;
    }

    @Transactional(readOnly = true)
    public List<DataEntry> findReferences(Long imageId) {
        ImageResource image = imageResourceRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("图片不存在"));
        return dataEntryRepository.findByColFeatureDescContaining(image.getUrl());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<DataEntry>> findReferencesBatch(List<Long> imageIds) {
        List<ImageResource> images = imageResourceRepository.findAllById(imageIds);
        Map<Long, String> urlMap = new HashMap<>();
        for (ImageResource img : images) {
            urlMap.put(img.getId(), img.getUrl());
        }
        Map<Long, List<DataEntry>> result = new HashMap<>();
        for (Long id : imageIds) {
            result.put(id, new ArrayList<>());
        }
        for (Map.Entry<Long, String> entry : urlMap.entrySet()) {
            List<DataEntry> matched = dataEntryRepository.findByColFeatureDescContaining(entry.getValue());
            result.get(entry.getKey()).addAll(matched);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findAllVersionReferences(Long imageId) {
        ImageResource image = imageResourceRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("图片不存在"));
        String imageUrl = image.getUrl();
        List<DataEntry> matchedEntries = dataEntryRepository.findByColFeatureDescContaining(imageUrl);
        List<DataVersion> versions = dataVersionRepository.findAll();
        Map<Long, String> versionMap = new HashMap<>();
        for (DataVersion v : versions) {
            versionMap.put(v.getId(), v.getVersionNo());
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (DataEntry e : matchedEntries) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", e.getId());
            item.put("colProductSystem", e.getColProductSystem());
            item.put("colBizCategory", e.getColBizCategory());
            item.put("colBizDomain", e.getColBizDomain());
            item.put("versionId", e.getVersionId());
            item.put("versionNo", versionMap.getOrDefault(e.getVersionId(), "未知"));
            result.add(item);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ReqItem> findReqReferences(Long imageId) {
        ImageResource image = imageResourceRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("图片不存在"));
        return reqItemRepository.findByDescriptionContaining(image.getUrl());
    }

    private void moveImageFile(ImageResource img) {
        try {
            String oldPath = img.getPath();
            if (oldPath == null) return;
            String urlPrefix = "/api/images/file/";
            String reqPrefix = "/api/requirements/file/";
            String baseUrl;
            String urlPath;
            if (img.getUrl() != null && img.getUrl().startsWith(urlPrefix)) {
                baseUrl = storagePath;
                urlPath = img.getUrl().substring(urlPrefix.length());
            } else if (img.getUrl() != null && img.getUrl().startsWith(reqPrefix)) {
                baseUrl = storagePath.replace("images", "requirements");
                urlPath = img.getUrl().substring(reqPrefix.length());
            } else {
                return;
            }
            String versionDir = img.getVersionId() != null ? String.valueOf(img.getVersionId()) : "0";
            String subPath = buildSubPath(img.getCategory(), img.getDomain(), img.getProduct());
            Path newPath = Paths.get(baseUrl, versionDir, subPath, img.getStoredName());
            Path oldFilePath = Paths.get(oldPath);
            if (!Files.exists(oldFilePath) || Files.exists(newPath)) return;
            Files.createDirectories(newPath.getParent());
            Files.move(oldFilePath, newPath, StandardCopyOption.REPLACE_EXISTING);
            img.setPath(newPath.toString());
            String prefix = (img.getUrl() != null && img.getUrl().startsWith(reqPrefix)) ? reqPrefix : urlPrefix;
            img.setUrl(prefix + versionDir + "/" + subPath + "/" + img.getStoredName());
        } catch (Exception e) {
            log.error("移动图片文件失败（DB已改，物理文件未对齐） imageId={} path={}: {}", img.getId(), img.getPath(), e.getMessage());
        }
    }

    /**
     * 根据 category/domain/product/storedName 重新计算 path/url 并 set 到实体（仅计算，不移动物理文件）。
     * 文件移动由调用方在事务提交后（afterCommit）执行，以保证 DB 与文件系统一致性。
     */
    private void recomputeImageLocation(ImageResource img) {
        String urlPrefix = "/api/images/file/";
        String reqPrefix = "/api/requirements/file/";
        String baseUrl;
        if (img.getUrl() != null && img.getUrl().startsWith(reqPrefix)) {
            baseUrl = storagePath.replace("images", "requirements");
        } else {
            baseUrl = storagePath;
        }
        String versionDir = img.getVersionId() != null ? String.valueOf(img.getVersionId()) : "0";
        String subPath = buildSubPath(img.getCategory(), img.getDomain(), img.getProduct());
        Path newPath = Paths.get(baseUrl, versionDir, subPath, img.getStoredName());
        img.setPath(newPath.toString());
        String prefix = (img.getUrl() != null && img.getUrl().startsWith(reqPrefix)) ? reqPrefix : urlPrefix;
        img.setUrl(prefix + versionDir + "/" + subPath + "/" + img.getStoredName());
    }

    private String getExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        return switch (contentType.toLowerCase()) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }

    private String buildSubPath(String category, String domain, String product) {
        StringBuilder sb = new StringBuilder();
        if (category != null && !category.isEmpty()) {
            sb.append(sanitizePath(category));
        }
        if (domain != null && !domain.isEmpty()) {
            if (!sb.isEmpty()) sb.append("/");
            sb.append(sanitizePath(domain));
        }
        if (product != null && !product.isEmpty()) {
            if (!sb.isEmpty()) sb.append("/");
            sb.append(sanitizePath(product));
        }
        return sb.toString();
    }

    private String stripCountSuffix(String val) {
        if (val == null) return null;
        return val.replaceAll("\\s*\\(\\d+\\)$", "").trim();
    }

    private String resolveL3Product(String product, String category, String domain, Long versionId) {
        if (product == null || product.isEmpty() || versionId == null) return product;
        List<DataEntry> l3Entries = dataEntryRepository.findByVersionIdAndLevelAndColBizCategoryAndColBizDomain(
                versionId, 3, category, domain);
        for (DataEntry e : l3Entries) {
            if (product.equals(e.getColProductSystem())) return product;
        }
        List<DataEntry> allByProduct = dataEntryRepository.findByVersionIdAndColProductSystem(versionId, product);
        for (DataEntry e : allByProduct) {
            DataEntry ancestor = findL3Ancestor(e);
            if (ancestor != null) return ancestor.getColProductSystem();
        }
        return product;
    }

    private DataEntry findL3Ancestor(DataEntry entry) {
        if (entry == null) return null;
        if (entry.getLevel() != null && entry.getLevel() == 3) return entry;
        Set<Long> visited = new HashSet<>();
        DataEntry current = entry;
        while (current != null && current.getParentId() != null) {
            if (visited.contains(current.getParentId())) break;
            visited.add(current.getParentId());
            Optional<DataEntry> parent = dataEntryRepository.findById(current.getParentId());
            if (parent.isEmpty()) break;
            current = parent.get();
            if (current.getLevel() != null && current.getLevel() == 3) return current;
        }
        return null;
    }

    private Long findL3EntryId(Long versionId, String productName) {
        List<DataEntry> l3Entries = dataEntryRepository.findByVersionIdAndLevel(versionId, 3);
        for (DataEntry e : l3Entries) {
            if (productName.equals(e.getColProductSystem())) return e.getId();
        }
        List<DataEntry> allByProduct = dataEntryRepository.findByVersionIdAndColProductSystem(versionId, productName);
        for (DataEntry e : allByProduct) {
            DataEntry l3 = findL3Ancestor(e);
            if (l3 != null) return l3.getId();
        }
        return null;
    }

    private String sanitizePath(String input) {
        return input.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    public String startMigration(List<Long> ids) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        MigrationTaskProgress progress = new MigrationTaskProgress();
        progress.setTaskId(taskId);
        progress.setStatus("RUNNING");
        progress.setTotalEntries(ids.size());
        progress.setProcessedEntries(0);
        progress.setSuccessImages(0);
        progress.setFailedImages(0);
        progress.setCurrentEntry("");
        taskStore.put(taskId, progress);
        self.migrateExternalImagesAsync(taskId, ids);
        return taskId;
    }

    public MigrationTaskProgress getMigrationProgress(String taskId) {
        return taskStore.get(taskId);
    }

    @Async
    public void migrateExternalImagesAsync(String taskId, List<Long> ids) {
        MigrationTaskProgress progress = taskStore.get(taskId);
        try {
            List<DataEntry> entries = dataEntryRepository.findAllById(ids);
            int successImages = 0;
            int failedImages = 0;
            int processedCount = 0;

            for (DataEntry entry : entries) {
                progress.setCurrentEntry(entry.getColProductSystem() != null ? entry.getColProductSystem() : "ID:" + entry.getId());
                String desc = entry.getColFeatureDesc();
                if (desc == null || desc.isEmpty() || !desc.contains("cloudimgs.jscloud.vip")) {
                    processedCount++;
                    progress.setProcessedEntries(processedCount);
                    continue;
                }

                String category = entry.getColBizCategory();
                if (category != null) category = category.trim();
                String domain = entry.getColBizDomain();
                if (domain != null) domain = domain.trim();

                DataEntry l3 = entry;
                while (l3 != null && l3.getLevel() != null && l3.getLevel() > 3) {
                    Long parentId = l3.getParentId();
                    l3 = parentId != null ? dataEntryRepository.findById(parentId).orElse(null) : null;
                }
                String product = l3 != null ? l3.getColProductSystem() : entry.getColProductSystem();
                if (product != null) product = product.trim();

                String newDesc = desc;
                newDesc = newDesc.replaceAll("<\\s+(https?://)", "<$1");
                newDesc = newDesc.replaceAll("<\\s*<\\s*(span\\s+class=\"image-card\")", "<$1");
                int entryMigrated = 0;

                Pattern extUrlPattern = Pattern.compile("https?://cloudimgs\\.jscloud\\.vip:\\d+/[^\"<>\\]]+");
                Map<String, ImageResource> downloaded = new HashMap<>();
                Matcher urlMatcher = extUrlPattern.matcher(newDesc);
                while (urlMatcher.find()) {
                    String extUrl = urlMatcher.group();
                    if (!downloaded.containsKey(extUrl)) {
                        ImageResource image = downloadAndStoreImage(extUrl, category, domain, product, entry.getVersionId());
                        downloaded.put(extUrl, image);
                    }
                }

                Pattern cardOpenPattern = Pattern.compile(
                    "<span\\s+class=\"image-card\"[^>]*?>", Pattern.DOTALL);
                Matcher cardMatcher = cardOpenPattern.matcher(newDesc);
                StringBuffer sbCards = new StringBuffer();
                int lastEnd = 0;
                while (cardMatcher.find()) {
                    int matchStart = cardMatcher.start();
                    sbCards.append(newDesc, lastEnd, matchStart);
                    String blockStart = cardMatcher.group();
                    String afterStart = newDesc.substring(cardMatcher.end());
                    String fullBlock = extractImageCardBlock(blockStart, afterStart);
                    lastEnd = matchStart + fullBlock.length();

                    Matcher urlInCard = extUrlPattern.matcher(fullBlock);
                    if (urlInCard.find()) {
                        String extUrl = urlInCard.group();
                        ImageResource image = downloaded.get(extUrl);
                        if (image != null) {
                            sbCards.append(updateImageCardUrls(fullBlock, extUrl, image));
                            entryMigrated++;
                            continue;
                        }
                    }
                    sbCards.append(fullBlock);
                }
                sbCards.append(newDesc, lastEnd, newDesc.length());
                newDesc = sbCards.toString();

                Pattern bracketPattern = Pattern.compile("\\[(https?://cloudimgs\\.jscloud\\.vip:\\d+/[^\\]]+)\\]");
                Matcher bracketMatcher = bracketPattern.matcher(newDesc);
                StringBuffer sbBracket = new StringBuffer();
                while (bracketMatcher.find()) {
                    String extUrl = bracketMatcher.group(1);
                    ImageResource image = downloaded.get(extUrl);
                    if (image != null) {
                        bracketMatcher.appendReplacement(sbBracket, Matcher.quoteReplacement(buildImageCard(image)));
                        entryMigrated++;
                    } else {
                        bracketMatcher.appendReplacement(sbBracket, Matcher.quoteReplacement(bracketMatcher.group(0)));
                    }
                }
                bracketMatcher.appendTail(sbBracket);
                newDesc = sbBracket.toString();

                Pattern anglePattern = Pattern.compile("<(https?://cloudimgs\\.jscloud\\.vip:\\d+/[^>]+)>");
                Matcher angleMatcher = anglePattern.matcher(newDesc);
                StringBuffer sbAngle = new StringBuffer();
                while (angleMatcher.find()) {
                    String extUrl = angleMatcher.group(1);
                    ImageResource image = downloaded.get(extUrl);
                    if (image != null) {
                        angleMatcher.appendReplacement(sbAngle, Matcher.quoteReplacement(buildImageCard(image)));
                        entryMigrated++;
                    } else {
                        angleMatcher.appendReplacement(sbAngle, Matcher.quoteReplacement(angleMatcher.group(0)));
                    }
                }
                angleMatcher.appendTail(sbAngle);
                newDesc = sbAngle.toString();

                Matcher plainUrlMatcher = extUrlPattern.matcher(newDesc);
                StringBuffer sbPlain = new StringBuffer();
                while (plainUrlMatcher.find()) {
                    String extUrl = plainUrlMatcher.group();
                    ImageResource image = downloaded.get(extUrl);
                    if (image != null) {
                        plainUrlMatcher.appendReplacement(sbPlain, Matcher.quoteReplacement(buildImageCard(image)));
                        entryMigrated++;
                    } else {
                        plainUrlMatcher.appendReplacement(sbPlain, Matcher.quoteReplacement(extUrl));
                    }
                }
                plainUrlMatcher.appendTail(sbPlain);
                newDesc = sbPlain.toString();

                if (!newDesc.equals(desc)) {
                    entry.setColFeatureDesc(newDesc);
                    dataEntryRepository.save(entry);
                }

                int entryFailed = 0;
                for (ImageResource img : downloaded.values()) {
                    if (img == null) entryFailed++;
                }
                successImages += entryMigrated;
                failedImages += entryFailed;
                if (entryFailed > 0) {
                    MigrationResult.EntryFailDetail detail = new MigrationResult.EntryFailDetail();
                    detail.setEntryId(entry.getId());
                    detail.setProductName(entry.getColProductSystem() != null ? entry.getColProductSystem() : (product != null ? product : "ID:" + entry.getId()));
                    detail.setFailedImageCount(entryFailed);
                    detail.setTotalImageCount(downloaded.size());
                    progress.getFailures().add(detail);
                }

                processedCount++;
                progress.setProcessedEntries(processedCount);
                progress.setSuccessImages(successImages);
                progress.setFailedImages(failedImages);
            }

            progress.setStatus("COMPLETED");
        } catch (Exception e) {
            progress.setStatus("FAILED");
        }
    }

    private String extractImageCardBlock(String blockStart, String after) {
        int depth = 1;
        int i = 0;
        int len = after.length();
        int tagCloseLen = "</span>".length();
        int tagOpenLen = "<span".length();
        while (i < len && depth > 0) {
            int nextClose = after.indexOf("</span>", i);
            int nextOpen = after.indexOf("<span", i);
            if (nextClose < 0) break;
            if (nextOpen >= 0 && nextOpen < nextClose) {
                depth++;
                i = nextOpen + tagOpenLen;
            } else {
                depth--;
                i = nextClose + tagCloseLen;
            }
        }
        return i > 0 ? blockStart + after.substring(0, i) : blockStart;
    }

    private String updateImageCardUrls(String block, String extUrl, ImageResource image) {
        String localUrl = image.getUrl();
        String block2 = block.replace("data-url=\"" + extUrl + "\"", "data-url=\"" + localUrl + "\"");
        block2 = block2.replace("src=\"" + extUrl + "\"", "src=\"" + localUrl + "\"");
        block2 = block2.replaceAll("data-id=\"[^\"]*\"", "data-id=\"" + image.getId() + "\"");
        if (!block2.contains("data-id=")) {
            block2 = block2.replaceFirst("class=\"image-card\"", "class=\"image-card\" data-id=\"" + image.getId() + "\"");
        }
        String name = image.getFilename() != null ? image.getFilename() : image.getStoredName();
        if (name != null) {
            String nameSafe = name.replace("\"", "&quot;").replace("'", "&#39;");
            block2 = block2.replaceAll("data-filename=\"[^\"]*\"", "data-filename=\"" + nameSafe + "\"");
            block2 = block2.replaceAll("title=\"[^\"]*\"", "title=\"" + nameSafe + "\"");
        }
        return block2;
    }

    private ImageResource downloadAndStoreImage(String url, String category, String domain, String product, Long versionId) {
        try {
            String encodedUrlStr = url;
            try {
                new java.net.URL(url);
                if (url.matches(".*[\\u4e00-\\u9fff\\s()（）].*")) {
                    int schemeEnd = url.indexOf("://");
                    if (schemeEnd >= 0) {
                        String scheme = url.substring(0, schemeEnd);
                        String rest = url.substring(schemeEnd + 3);
                        int pathStart = rest.indexOf('/');
                        String hostPort = pathStart >= 0 ? rest.substring(0, pathStart) : rest;
                        String pathQuery = pathStart >= 0 ? rest.substring(pathStart) : "";
                        String[] segments = pathQuery.split("/", -1);
                        StringBuilder sb = new StringBuilder();
                        for (String seg : segments) {
                            if (seg.isEmpty()) continue;
                            sb.append("/").append(java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20"));
                        }
                        encodedUrlStr = scheme + "://" + hostPort + sb;
                    }
                }
            } catch (java.net.MalformedURLException e) {
                try {
                    encodedUrlStr = new java.net.URI(url).toASCIIString();
                } catch (Exception ex) {
                    int schemeEnd = url.indexOf("://");
                    if (schemeEnd >= 0) {
                        String scheme = url.substring(0, schemeEnd);
                        String rest = url.substring(schemeEnd + 3);
                        int pathStart = rest.indexOf('/');
                        String hostPort = pathStart >= 0 ? rest.substring(0, pathStart) : rest;
                        String pathQuery = pathStart >= 0 ? rest.substring(pathStart) : "";
                        String[] segments = pathQuery.split("/", -1);
                        StringBuilder sb = new StringBuilder();
                        for (String seg : segments) {
                            if (seg.isEmpty()) continue;
                            sb.append("/").append(java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20"));
                        }
                        encodedUrlStr = scheme + "://" + hostPort + sb;
                    }
                }
            }
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(encodedUrlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != 200) return null;
            String contentType = conn.getContentType();
            if (contentType != null && contentType.startsWith("text/")) return null;
            byte[] data = conn.getInputStream().readAllBytes();
            if (data.length == 0) return null;

            String resolvedProduct = resolveL3Product(product, category, domain, versionId);
            String subPath = buildSubPath(category, domain, resolvedProduct);
            String versionDir = versionId != null ? String.valueOf(versionId) : "0";
            Path dirPath = Paths.get(storagePath, versionDir, subPath);
            Files.createDirectories(dirPath);

            String filename;
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash >= 0) {
                filename = java.net.URLDecoder.decode(url.substring(lastSlash + 1), "UTF-8");
            } else {
                filename = UUID.randomUUID() + ".png";
            }
            filename = sanitizePath(filename);
            if (!filename.matches(".*\\.(png|jpg|jpeg|gif|webp|bmp)$")) {
                filename += ".png";
            }

            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            String storedName = filename;
            Path filePath = dirPath.resolve(storedName);
            Files.write(filePath, data);

            String migUrlPath = "/api/images/file/" + versionDir + "/" + subPath + "/" + storedName;

            ImageResource image = new ImageResource();
            image.setFilename(filename);
            image.setStoredName(storedName);
            image.setPath(filePath.toString());
            image.setCategory(category);
            image.setDomain(domain);
            image.setProduct(resolvedProduct);
            image.setUrl(migUrlPath);
            image.setSize((long) data.length);
            image.setMimeType(contentType != null ? contentType : "image/png");
            image.setUploadedBy("migration");
            image.setVersionId(versionId);
            imageResourceRepository.save(image);

            return image;
        } catch (Exception e) {
            System.out.println("[Migration] Failed to download: " + url + " - " + e.getMessage());
            return null;
        }
    }

    private String buildImageCard(ImageResource image) {
        String url = image.getUrl();
        String name = image.getFilename() != null ? image.getFilename() : image.getStoredName();
        if (name == null) name = "图片";
        String nameSafe = name.replace("\"", "&quot;").replace("'", "&#39;");
        String sizeStr = formatSizeString(image.getSize());
        return "<span class=\"image-card\" contenteditable=\"false\" data-url=\"" + url
            + "\" data-filename=\"" + nameSafe + "\" data-id=\"" + image.getId()
            + "\" title=\"" + nameSafe + "\"><span class=\"image-thumb\"><img src=\"" + url
            + "\" alt=\"" + nameSafe + "\" /></span><span class=\"image-info\"><button type=\"button\""
            + " class=\"image-action-btn image-edit-name-btn\" data-action=\"edit-name\">编辑</button>"
            + "<span class=\"image-name\">" + nameSafe + "</span><span class=\"image-size\">"
            + sizeStr + "</span></span><span class=\"image-actions\"><button type=\"button\""
            + " class=\"image-action-btn\" data-action=\"preview\">预览</button><button type=\"button\""
            + " class=\"image-action-btn image-action-danger\" data-action=\"delete\">删除</button>"
            + "<button type=\"button\" class=\"image-action-btn\" data-action=\"replace\">替换</button></span></span>";
    }

    private String formatSizeString(Long bytes) {
        if (bytes == null) return "";
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1f", bytes / 1024.0) + "KB";
        return String.format("%.1f", bytes / (1024.0 * 1024.0)) + "MB";
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Async
    @Transactional
    public void backfillImageDimensions() {
        List<ImageResource> images = imageResourceRepository.findAll().stream()
                .filter(img -> img.getWidth() == null || img.getHeight() == null)
                .toList();
        if (images.isEmpty()) {
            log.info("所有图片宽高已存在，无需回填");
            return;
        }
        log.info("开始回填图片宽高，共 {} 张", images.size());
        int count = 0;
        for (ImageResource img : images) {
            try {
                Path filePath = Paths.get(img.getPath());
                if (Files.exists(filePath)) {
                    java.awt.image.BufferedImage bimg = javax.imageio.ImageIO.read(filePath.toFile());
                    if (bimg != null) {
                        img.setWidth(bimg.getWidth());
                        img.setHeight(bimg.getHeight());
                        imageResourceRepository.save(img);
                        count++;
                        if (count % 500 == 0) {
                            log.info("已回填 {} / {} 张图片宽高", count, images.size());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("回填图片宽高失败 id={} path={}: {}", img.getId(), img.getPath(), e.getMessage());
            }
        }
        log.info("图片宽高回填完成，成功 {} / {} 张", count, images.size());
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageResourceService.class);

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}