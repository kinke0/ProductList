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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    private final ConcurrentHashMap<String, MigrationTaskProgress> taskStore = new ConcurrentHashMap<>();

    @Lazy
    private final ImageResourceService self;

    public ImageResourceService(ImageResourceRepository imageResourceRepository,
                                DataEntryRepository dataEntryRepository,
                                BaseCategoryRepository baseCategoryRepository,
                                BaseDomainRepository baseDomainRepository,
                                @Lazy ImageResourceService self) {
        this.imageResourceRepository = imageResourceRepository;
        this.dataEntryRepository = dataEntryRepository;
        this.baseCategoryRepository = baseCategoryRepository;
        this.baseDomainRepository = baseDomainRepository;
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

        String originalFilename = file.getOriginalFilename();
        String ext = getExtension(originalFilename, contentType);
        String effectiveName = (displayName != null && !displayName.isEmpty()) ? displayName : originalFilename;
        String sanitizedBase = sanitizePath(effectiveName);
        if (sanitizedBase.isEmpty()) sanitizedBase = UUID.randomUUID().toString();
        String storedName = sanitizedBase + "." + ext;

        String subPath = buildSubPath(category, domain, product);
        Path dirPath = Paths.get(storagePath, subPath);
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

        String urlPath = "/api/images/file/" + subPath + "/" + storedName;

        ImageResource image = new ImageResource();
        image.setFilename(displayName != null && !displayName.isEmpty() ? displayName : originalFilename);
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

        return imageResourceRepository.save(image);
    }

    public List<ImageResource> findAll(String category, String domain, String product, Long versionId) {
        final String cat = stripCountSuffix(category);
        final String dom = stripCountSuffix(domain);
        final String prod = stripCountSuffix(product);
        List<ImageResource> direct = findDirect(cat, dom, prod, versionId);

        if (versionId != null && cat != null && dom != null) {
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

    private List<ImageResource> findDirect(String category, String domain, String product, Long versionId) {
        if (versionId != null && category != null && domain != null && product != null) {
            return imageResourceRepository.findByVersionIdAndCategoryAndDomainAndProduct(versionId, category, domain, product);
        }
        if (versionId != null && category != null && domain != null) {
            return imageResourceRepository.findByVersionIdAndCategoryAndDomain(versionId, category, domain);
        }
        if (versionId != null && category != null) {
            return imageResourceRepository.findByVersionIdAndCategory(versionId, category);
        }
        if (versionId != null) {
            return imageResourceRepository.findByVersionId(versionId);
        }
        if (category != null && domain != null && product != null) {
            return imageResourceRepository.findByCategoryAndDomainAndProduct(category, domain, product);
        }
        if (category != null && domain != null) {
            return imageResourceRepository.findByCategoryAndDomain(category, domain);
        }
        if (category != null) {
            return imageResourceRepository.findByCategory(category);
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
        List<ImageResource> allImages = imageResourceRepository.findByVersionId(versionId);
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

        try {
            Path filePath = Paths.get(image.getPath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new BusinessException("文件删除失败: " + e.getMessage());
        }

        imageResourceRepository.deleteById(id);
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
        if (body.getFilename() != null) {
            String newName = body.getFilename();
            String ext = image.getStoredName().substring(image.getStoredName().lastIndexOf('.') + 1);
            String newStored = sanitizePath(newName);
            if (newStored.isEmpty()) newStored = UUID.randomUUID().toString();
            if (!newStored.endsWith("." + ext)) newStored = newStored + "." + ext;
            if (!newStored.equals(image.getStoredName())) {
                List<ImageResource> dup = imageResourceRepository.findByCategoryAndDomainAndProductAndStoredName(
                    image.getCategory(), image.getDomain(), image.getProduct(), newStored);
                if (!dup.isEmpty()) {
                    throw new BusinessException("同目录下已存在同名文件: " + newName);
                }
                String oldPath = image.getPath();
                Path newPath = Paths.get(image.getPath()).resolveSibling(newStored);
                try {
                    Files.move(Paths.get(oldPath), newPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BusinessException("文件重命名失败: " + e.getMessage());
                }
                image.setStoredName(newStored);
                image.setPath(newPath.toString());
                image.setFilename(newName);
                String subPath = buildSubPath(image.getCategory(), image.getDomain(), image.getProduct());
                image.setUrl("/api/images/file/" + subPath + "/" + newStored);
            } else {
                image.setFilename(newName);
            }
        }
        if (body.getCategory() != null) {
            image.setCategory(body.getCategory());
        }
        if (body.getDomain() != null) {
            image.setDomain(body.getDomain());
        }
        if (body.getProduct() != null) {
            image.setProduct(body.getProduct());
        }
        return imageResourceRepository.save(image);
    }

    public List<ImageDirectoryNode> getTree(Long versionId) {
        List<ImageResource> images;
        if (versionId != null) {
            images = imageResourceRepository.findByVersionId(versionId);
        } else {
            images = imageResourceRepository.findAll();
        }

        Map<String, Integer> imageCountByCat = new LinkedHashMap<>();
        Map<String, Integer> imageCountByDom = new LinkedHashMap<>();
        Map<String, Integer> imageCountByProd = new LinkedHashMap<>();
        for (ImageResource img : images) {
            String cat = img.getCategory() != null ? img.getCategory() : "";
            String dom = img.getDomain() != null ? img.getDomain() : "";
            String prod = img.getProduct() != null ? img.getProduct() : "";
            imageCountByCat.merge(cat, 1, Integer::sum);
            String domKey = cat + "||" + dom;
            imageCountByDom.merge(domKey, 1, Integer::sum);
            String prodKey = domKey + "||" + prod;
            imageCountByProd.merge(prodKey, 1, Integer::sum);
        }

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
            Map<Long, DataEntry> entryMap = new HashMap<>();
            for (DataEntry e : entries) entryMap.put(e.getId(), e);
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

        List<ImageDirectoryNode> roots = new ArrayList<>();
        for (String catName : orderedCategories) {
            ImageDirectoryNode catNode = new ImageDirectoryNode();
            int catCount = imageCountByCat.getOrDefault(catName, 0);
            catNode.setLabel(catName + " (" + catCount + ")");
            catNode.setCount(catCount);

            List<ImageDirectoryNode> domainNodes = new ArrayList<>();
            List<String> domains = catToDomains.getOrDefault(catName, new ArrayList<>());
            for (String domName : domains) {
                String domKey = catName + "||" + domName;
                int domCount = imageCountByDom.getOrDefault(domKey, 0);
                ImageDirectoryNode domNode = new ImageDirectoryNode();
                domNode.setLabel(domName + " (" + domCount + ")");
                domNode.setCount(domCount);

                List<ImageDirectoryNode> prodNodes = new ArrayList<>();
                List<String> products = domToProducts.getOrDefault(domKey, new ArrayList<>());
                for (String prodName : products) {
                    String prodKey = domKey + "||" + prodName;
                    int prodCount = imageCountByProd.getOrDefault(prodKey, 0);
                    ImageDirectoryNode prodNode = new ImageDirectoryNode();
                    prodNode.setLabel(prodName + " (" + prodCount + ")");
                    prodNode.setCount(prodCount);
                    prodNode.setChildren(null);
                    prodNodes.add(prodNode);
                }
                domNode.setChildren(prodNodes.isEmpty() ? null : prodNodes);
                domainNodes.add(domNode);
            }
            catNode.setChildren(domainNodes.isEmpty() ? null : domainNodes);
            roots.add(catNode);
        }

        return roots;
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

        List<DataEntry> allEntries = dataEntryRepository.findAll();
        String imageUrl = image.getUrl();

        return allEntries.stream()
                .filter(e -> {
                    String desc = e.getColFeatureDesc();
                    return desc != null && desc.contains(imageUrl);
                })
                .collect(Collectors.toList());
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

            String subPath = buildSubPath(category, domain, product);
            Path dirPath = Paths.get(storagePath, subPath);
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

            String urlPath = "/api/images/file/" + subPath + "/" + storedName;

            ImageResource image = new ImageResource();
            image.setFilename(filename);
            image.setStoredName(storedName);
            image.setPath(filePath.toString());
            image.setCategory(category);
            image.setDomain(domain);
            image.setProduct(product);
            image.setUrl(urlPath);
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

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}