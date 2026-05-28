package com.superpower.modules.image.service;

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
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageResourceService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private final ImageResourceRepository imageResourceRepository;
    private final DataEntryRepository dataEntryRepository;

    @Value("${app.image-storage-path:./uploads/images}")
    private String storagePath;

    public ImageResourceService(ImageResourceRepository imageResourceRepository,
                                DataEntryRepository dataEntryRepository) {
        this.imageResourceRepository = imageResourceRepository;
        this.dataEntryRepository = dataEntryRepository;
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
        String storedName = UUID.randomUUID() + "." + ext;

        String subPath = buildSubPath(category, domain, product);
        Path dirPath = Paths.get(storagePath, subPath);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            throw new BusinessException("创建目录失败: " + e.getMessage());
        }

        Path filePath = dirPath.resolve(storedName);
        try {
            Files.copy(file.getInputStream(), filePath);
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

    public List<ImageDirectoryNode> getTree(Long versionId) {
        List<ImageResource> images;
        if (versionId != null) {
            images = imageResourceRepository.findByVersionId(versionId);
        } else {
            images = imageResourceRepository.findAll();
        }

        Map<String, Map<String, Map<String, List<ImageResource>>>> treeMap = new LinkedHashMap<>();
        for (ImageResource img : images) {
            String cat = img.getCategory() != null ? img.getCategory() : "未分类";
            String dom = img.getDomain() != null ? img.getDomain() : "未分类";
            String prod = img.getProduct() != null ? img.getProduct() : "未分类";
            treeMap.computeIfAbsent(cat, k -> new LinkedHashMap<>())
                    .computeIfAbsent(dom, k -> new LinkedHashMap<>())
                    .computeIfAbsent(prod, k -> new ArrayList<>())
                    .add(img);
        }

        List<ImageDirectoryNode> roots = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, List<ImageResource>>>> catEntry : treeMap.entrySet()) {
            ImageDirectoryNode catNode = new ImageDirectoryNode();
            catNode.setLabel(catEntry.getKey());
            List<ImageDirectoryNode> domainNodes = new ArrayList<>();
            int catCount = 0;

            for (Map.Entry<String, Map<String, List<ImageResource>>> domEntry : catEntry.getValue().entrySet()) {
                ImageDirectoryNode domNode = new ImageDirectoryNode();
                domNode.setLabel(domEntry.getKey());
                List<ImageDirectoryNode> prodNodes = new ArrayList<>();
                int domCount = 0;

                for (Map.Entry<String, List<ImageResource>> prodEntry : domEntry.getValue().entrySet()) {
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