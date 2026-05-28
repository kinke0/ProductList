package com.superpower.modules.image.service;

import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.common.BusinessException;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.image.dto.ImageDirectoryNode;
import com.superpower.modules.image.entity.ImageResource;
import com.superpower.modules.image.repository.ImageResourceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageResourceService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final ImageResourceRepository imageResourceRepository;
    private final DataEntryRepository dataEntryRepository;
    private final BaseCategoryRepository baseCategoryRepository;

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    public ImageResourceService(ImageResourceRepository imageResourceRepository,
                                DataEntryRepository dataEntryRepository,
                                BaseCategoryRepository baseCategoryRepository) {
        this.imageResourceRepository = imageResourceRepository;
        this.dataEntryRepository = dataEntryRepository;
        this.baseCategoryRepository = baseCategoryRepository;
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
        int dup = 1;
        while (Files.exists(filePath)) {
            storedName = sanitizedBase + "_" + dup + "." + ext;
            filePath = dirPath.resolve(storedName);
            dup++;
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
        List<ImageResource> direct = findDirect(category, domain, product, versionId);

        if (versionId != null && category != null && domain != null) {
            List<DataEntry> matchingEntries = dataEntryRepository.findByVersionIdAndColBizCategoryAndColBizDomain(
                    versionId, category, domain);
            if (product != null) {
                Map<Long, DataEntry> entryMap = new HashMap<>();
                for (DataEntry e : dataEntryRepository.findByVersionId(versionId)) {
                    entryMap.put(e.getId(), e);
                }
                matchingEntries = matchingEntries.stream()
                        .filter(e -> {
                            DataEntry l3 = findAncestorAtLevel(e, entryMap, 3);
                            return l3 != null && product.equals(l3.getColProductSystem());
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

    @Transactional
    public ImageResource update(Long id, ImageResource body) {
        ImageResource image = imageResourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("图片不存在"));
        if (body.getFilename() != null) {
            image.setFilename(body.getFilename());
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

        Map<String, Map<String, Map<String, Set<Long>>>> treeImgIds = new LinkedHashMap<>();
        Map<Long, ImageResource> imgMap = new LinkedHashMap<>();
        for (ImageResource img : images) {
            imgMap.put(img.getId(), img);
            String cat = img.getCategory() != null ? img.getCategory() : "未分类";
            String dom = img.getDomain() != null ? img.getDomain() : "未分类";
            String prod = img.getProduct() != null ? img.getProduct() : "未分类";
            treeImgIds.computeIfAbsent(cat, k -> new LinkedHashMap<>())
                    .computeIfAbsent(dom, k -> new LinkedHashMap<>())
                    .computeIfAbsent(prod, k -> new LinkedHashSet<>())
                    .add(img.getId());
        }

        if (versionId != null) {
            Map<Long, DataEntry> entryMap = new HashMap<>();
            for (DataEntry e : dataEntryRepository.findByVersionId(versionId)) {
                entryMap.put(e.getId(), e);
            }
            for (DataEntry entry : entryMap.values()) {
                String desc = entry.getColFeatureDesc();
                if (desc == null) continue;
                String entryCat = entry.getColBizCategory();
                String entryDom = entry.getColBizDomain();
                if (entryCat == null || entryDom == null) continue;
                DataEntry l3 = findAncestorAtLevel(entry, entryMap, 3);
                String entryProd = l3 != null ? l3.getColProductSystem() : entry.getColProductSystem();
                if (entryProd == null) continue;
                for (ImageResource img : images) {
                    String url = img.getUrl();
                    if (url != null && desc.contains(url)) {
                        if (!entryCat.equals(img.getCategory()) || !entryDom.equals(img.getDomain()) || !entryProd.equals(img.getProduct())) {
                            treeImgIds.computeIfAbsent(entryCat, k -> new LinkedHashMap<>())
                                    .computeIfAbsent(entryDom, k -> new LinkedHashMap<>())
                                    .computeIfAbsent(entryProd, k -> new LinkedHashSet<>())
                                    .add(img.getId());
                        }
                    }
                }
            }
        }

        List<String> orderedCategories = new ArrayList<>();
        if (versionId != null) {
            List<BaseCategory> baseCategories = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
            for (BaseCategory bc : baseCategories) {
                if (treeImgIds.containsKey(bc.getName())) {
                    orderedCategories.add(bc.getName());
                }
            }
        }
        for (String cat : treeImgIds.keySet()) {
            if (!orderedCategories.contains(cat)) {
                orderedCategories.add(cat);
            }
        }

        List<ImageDirectoryNode> roots = new ArrayList<>();
        for (String catName : orderedCategories) {
            Map<String, Map<String, Set<Long>>> domMap = treeImgIds.get(catName);
            ImageDirectoryNode catNode = new ImageDirectoryNode();
            catNode.setLabel(catName);
            List<ImageDirectoryNode> domainNodes = new ArrayList<>();
            int catCount = 0;

            for (Map.Entry<String, Map<String, Set<Long>>> domEntry : domMap.entrySet()) {
                ImageDirectoryNode domNode = new ImageDirectoryNode();
                domNode.setLabel(domEntry.getKey());
                List<ImageDirectoryNode> prodNodes = new ArrayList<>();
                int domCount = 0;

                for (Map.Entry<String, Set<Long>> prodEntry : domEntry.getValue().entrySet()) {
                    ImageDirectoryNode prodNode = new ImageDirectoryNode();
                    prodNode.setLabel(prodEntry.getKey());
                    prodNode.setCount(prodEntry.getValue().size());
                    prodNode.setChildren(null);
                    prodNodes.add(prodNode);
                    domCount += prodEntry.getValue().size();
                }

                domNode.setChildren(prodNodes);
                domNode.setCount(domCount);
                domainNodes.add(domNode);
                catCount += domCount;
            }

            catNode.setChildren(domainNodes);
            catNode.setCount(catCount);
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

    private String sanitizePath(String input) {
        return input.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}