package com.superpower.modules.document.service;

import com.superpower.modules.customtab.entity.CustomTabEntry;
import com.superpower.modules.customtab.repository.CustomTabEntryRepository;
import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.category.entity.BaseCategory;
import com.superpower.modules.category.entity.BaseDomain;
import com.superpower.modules.category.repository.BaseCategoryRepository;
import com.superpower.modules.category.repository.BaseDomainRepository;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.repository.DocGenRecordRepository;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final String FONT_NAME = "宋体";
    private static final int MAX_HEADING_LEVEL = 9;
    private static final double PORTRAIT_RATIO = 1.2;
    private static final int IMAGE_LANDSCAPE_WIDTH_PX = 500;
    private static final int IMAGE_PORTRAIT_HEIGHT_PX = 300;
    private static final double COMPRESS_DPI_RATIO = 2.4;
    private static final Pattern URL_PATTERN = Pattern.compile("\\[([^\\[\\]]+)\\]");
    private static final Pattern IMAGE_CARD_PATTERN = Pattern.compile(
            "<(?:span|div)[^>]+class=\"(?:image-card|img-card)\"[^>]*>",
            Pattern.DOTALL);
    private static final Pattern IMAGE_CARD_END_PATTERN = Pattern.compile(
            "</(?:span|div)>(?:\\s*<br\\s*/?>)?",
            Pattern.DOTALL);
    private static final Pattern URL_ATTR_PATTERN = Pattern.compile(
            "data-url=\"([^\"]+)\"");
    private static final Pattern FILENAME_ATTR_PATTERN = Pattern.compile(
            "data-filename=\"([^\"]+)\"");
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");

    private final DataEntryRepository entryRepository;
    private final DocGenRecordRepository genRecordRepository;
    private final CustomTabEntryRepository customTabEntryRepository;
    private final BaseCategoryRepository baseCategoryRepository;
    private final BaseDomainRepository baseDomainRepository;
    private final ConcurrentHashMap<Long, Boolean> cancelledRecords = new ConcurrentHashMap<>();

    @Value("${app.doc-storage-path:./generated-docs}")
    private String docStoragePath;

    @Value("${app.image-storage-path:./uploads/images}")
    private String imageStoragePath;

    public DocumentService(DataEntryRepository entryRepository, DocGenRecordRepository genRecordRepository,
                           CustomTabEntryRepository customTabEntryRepository,
                           BaseCategoryRepository baseCategoryRepository, BaseDomainRepository baseDomainRepository) {
        this.entryRepository = entryRepository;
        this.genRecordRepository = genRecordRepository;
        this.customTabEntryRepository = customTabEntryRepository;
        this.baseCategoryRepository = baseCategoryRepository;
        this.baseDomainRepository = baseDomainRepository;
    }

    private boolean isCancelled(Long recordId) {
        return recordId != null && cancelledRecords.getOrDefault(recordId, false);
    }

    public void cancelGeneration(Long recordId) {
        cancelledRecords.put(recordId, true);
    }

    private void saveGenRecordWithRetry(DocGenRecord record, int maxRetries) {
        for (int retry = 0; retry < maxRetries; retry++) {
            try {
                genRecordRepository.save(record);
                return;
            } catch (Exception e) {
                log.warn("saveGenRecord retry {}/{}: id={}, error={}", retry + 1, maxRetries, record.getId(), e.getMessage());
                if (retry < maxRetries - 1) {
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { return; }
                }
            }
        }
        log.error("saveGenRecord failed after {} retries: id={}", maxRetries, record.getId());
    }

    private void saveGenRecordWithRetry(DocGenRecord record) {
        saveGenRecordWithRetry(record, 5);
    }

    public DocGenRecord createGenRecord(Long versionId, String docName, String docType, String format,
                                        List<Long> entryIds, Long userId, String userName) {
        DocGenRecord record = new DocGenRecord();
        record.setVersionId(versionId);
        record.setDocName(docName);
        record.setDocType(docType);
        record.setFormat(format);
        record.setEntryIds(entryIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        record.setGeneratedBy(userId);
        record.setGeneratedByName(userName);
        record.setStatus("generating");
        record.setTotalEntries(0);
        record.setProcessedEntries(0);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        saveGenRecordWithRetry(record);
        cancelledRecords.remove(record.getId());
        return record;
    }

    public void updateGenRecordSuccess(Long recordId, String filePath, long fileSize) {
        for (int retry = 0; retry < 10; retry++) {
            try {
                Optional<DocGenRecord> opt = genRecordRepository.findById(recordId);
                if (opt.isPresent()) {
                    DocGenRecord record = opt.get();
                    record.setFilePath(filePath);
                    record.setFileSize(fileSize);
                    record.setStatus("completed");
                    record.setProcessedEntries(record.getTotalEntries());
                    record.setUpdatedAt(LocalDateTime.now());
                    genRecordRepository.save(record);
                    cancelledRecords.remove(recordId);
                }
                log.info("updateGenRecordSuccess: recordId={}, filePath={}, fileSize={}", recordId, filePath, fileSize);
                return;
            } catch (Exception e) {
                log.warn("updateGenRecordSuccess failed (retry {}): recordId={}, error={}", retry + 1, recordId, e.getMessage());
                if (retry < 9) {
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { return; }
                }
            }
        }
        log.error("updateGenRecordSuccess failed after 10 retries: recordId={}", recordId);
    }

    public void updateGenRecordError(Long recordId, String errorMessage) {
        log.error("文档生成失败: recordId={}, error={}", recordId, errorMessage);
        for (int retry = 0; retry < 10; retry++) {
            try {
                Optional<DocGenRecord> opt = genRecordRepository.findById(recordId);
                if (opt.isPresent()) {
                    DocGenRecord record = opt.get();
                    record.setErrorMessage(errorMessage);
                    record.setStatus("error");
                    record.setUpdatedAt(LocalDateTime.now());
                    genRecordRepository.save(record);
                    cancelledRecords.remove(recordId);
                }
                return;
            } catch (Exception e) {
                log.warn("updateGenRecordError failed (retry {}): recordId={}, error={}", retry + 1, recordId, e.getMessage());
                if (retry < 9) {
                    try { Thread.sleep(1000); } catch (InterruptedException ie) { return; }
                }
            }
        }
        log.error("updateGenRecordError failed after 10 retries: recordId={}", recordId);
    }

    private final ConcurrentHashMap<Long, Integer> lastSavedProgress = new ConcurrentHashMap<>();

    public void updateGenRecordProgress(Long recordId, int processed, int total) {
        if (recordId == null) return;
        try {
            int last = lastSavedProgress.getOrDefault(recordId, -1);
            int step = Math.max(1, total / 50);
            if (processed - last < step && processed != total) return;
            for (int retry = 0; retry < 3; retry++) {
                try {
                    genRecordRepository.findById(recordId).ifPresent(record -> {
                        record.setProcessedEntries(processed);
                        record.setUpdatedAt(LocalDateTime.now());
                        genRecordRepository.save(record);
                        lastSavedProgress.put(recordId, processed);
                    });
                    break;
                } catch (Exception e) {
                    log.warn("updateGenRecordProgress retry {}: recordId={}, error={}", retry + 1, recordId, e.getMessage());
                    if (retry < 2) try { Thread.sleep(500); } catch (InterruptedException ie) { return; }
                }
            }
            if (processed >= total) lastSavedProgress.remove(recordId);
        } catch (Exception e) {
            log.warn("updateGenRecordProgress failed: recordId={}, processed={}, error={}", recordId, processed, e.getMessage());
        }
    }

    public List<DocGenRecord> getGenRecords(Long versionId) {
        return genRecordRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
    }

    public DocGenRecord getGenRecord(Long id) {
        return genRecordRepository.findById(id).orElse(null);
    }

    public void deleteGenRecord(Long id) {
        DocGenRecord record = genRecordRepository.findById(id).orElse(null);
        if (record == null) return;
        if ("generating".equals(record.getStatus())) {
            cancelGeneration(id);
        }
        if (record.getFilePath() != null) {
            try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(record.getFilePath())); } catch (Exception ignored) {}
        }
        genRecordRepository.deleteById(id);
    }

    public byte[] generateDocument(String docType, String format, List<Long> entryIds, Boolean includeImages) throws Exception {
        List<DataEntry> entries = new ArrayList<>(entryRepository.findAllById(entryIds));
        entries.sort(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0));
        if ("word".equals(format)) {
            return generateWord(docType, entries, null, includeImages, false);
        } else {
            return generateExcel(docType, entries, null);
        }
    }

    public String generateAndSaveDocument(Long recordId, String docType, String format,
                                          List<Long> entryIds, Long versionId, Long customTabId,
                                           Boolean includeImages, Boolean compressImages) throws Exception {
        cancelledRecords.remove(recordId);
        log.info("generateAndSaveDocument开始: recordId={}, docType={}, format={}", recordId, docType, format);
        List<DataEntry> entries;
        if (entryIds == null || entryIds.isEmpty()) {
            if (customTabId != null) {
                log.info("按自定义清单加载: customTabId={}, versionId={}", customTabId, versionId);
                entries = entryRepository.findEntriesByTab(versionId, customTabId);
            } else {
                log.info("按版本加载: versionId={}", versionId);
                entries = entryRepository.findAllEntries(versionId);
            }
        } else {
            log.info("按ID列表加载: count={}", entryIds.size());
            entries = new ArrayList<>(entryRepository.findAllById(entryIds));
            entries.sort(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0));
        }
        int totalSize = entries.size();
        log.info("数据加载完成: recordId={}, totalSize={}", recordId, totalSize);
        genRecordRepository.findById(recordId).ifPresent(r -> {
            r.setTotalEntries(totalSize);
            r.setProcessedEntries(0);
            r.setUpdatedAt(LocalDateTime.now());
            saveGenRecordWithRetry(r);
        });

        Path dir = Paths.get(docStoragePath);
        Files.createDirectories(dir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = "word".equals(format) ? ".docx" : ".xlsx";
        String filename = docType + "_" + timestamp + "_" + recordId + ext;
        Path filePath = dir.resolve(filename);

        if ("word".equals(format)) {
            generateWordToFile(docType, entries, recordId, filePath, includeImages, compressImages);
        } else {
            int filteredSize = (int) entries.stream().filter(e -> e.getLevel() != null && e.getLevel() >= 3).count();
            log.info("Excel生成: recordId={}, filteredSize={}", recordId, filteredSize);
            genRecordRepository.findById(recordId).ifPresent(r -> {
                r.setTotalEntries(filteredSize);
                saveGenRecordWithRetry(r);
            });
            byte[] data = generateExcel(docType, entries, recordId);
            log.info("Excel生成完成，写入文件: recordId={}, size={}KB", recordId, data.length / 1024);
            Files.write(filePath, data);
        }

        if (isCancelled(recordId)) {
            try { Files.deleteIfExists(filePath); } catch (Exception ignored) {}
            log.info("任务已取消，清除标记: recordId={}", recordId);
            cancelledRecords.remove(recordId);
            return null;
        }

        log.info("更新状态为completed: recordId={}, filePath={}", recordId, filePath);
        updateGenRecordSuccess(recordId, filePath.toString(), Files.size(filePath));
        return filePath.toString();
    }

    private void collectDescendants(List<Long> parentIds, List<DataEntry> allEntries) {
        Map<Long, List<DataEntry>> childrenByParent = new HashMap<>();
        for (DataEntry e : allEntries) {
            if (e.getParentId() != null) {
                childrenByParent.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
            }
        }
        boolean added;
        do {
            added = false;
            int size = parentIds.size();
            for (int i = 0; i < size; i++) {
                List<DataEntry> children = childrenByParent.get(parentIds.get(i));
                if (children != null) {
                    for (DataEntry child : children) {
                        if (!parentIds.contains(child.getId())) {
                            parentIds.add(child.getId());
                            added = true;
                        }
                    }
                }
            }
        } while (added);
    }

    private byte[] generateWord(String docType, List<DataEntry> entries, Long recordId, Boolean includeImages, Boolean compressImages) throws Exception {
        ZipSecureFile.setMinInflateRatio(0.001);
        ZipSecureFile.setMaxFileCount(100000);
        XWPFDocument doc = new XWPFDocument();
        ensureBuiltinHeadingStyles(doc);
        generateFeatureWord(doc, entries, recordId, docType, includeImages, compressImages);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return out.toByteArray();
    }

    private void generateWordToFile(String docType, List<DataEntry> entries, Long recordId, Path filePath,
                                     Boolean includeImages, Boolean compressImages) throws Exception {
        ZipSecureFile.setMinInflateRatio(0.001);
        ZipSecureFile.setMaxFileCount(100000);
        XWPFDocument doc = new XWPFDocument();
        ensureBuiltinHeadingStyles(doc);
        generateFeatureWord(doc, entries, recordId, docType, includeImages, compressImages);
        try (OutputStream out = Files.newOutputStream(filePath)) {
            doc.write(out);
        } finally {
            doc.close();
        }
    }

    private void addParagraph(XWPFDocument doc, String text) {
        XWPFParagraph para = doc.createParagraph();
        para.setIndentationFirstLine(420);
        para.setSpacingBetween(1.5);
        XWPFRun run = para.createRun();
        run.setText(text);
        setFontStyle(run);
    }

    private void generateBidWord(XWPFDocument doc, List<DataEntry> entries, Long recordId) {
        for (int i = 0; i < entries.size(); i++) {
            DataEntry e = entries.get(i);
            if (i > 0) doc.createParagraph();

            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.LEFT);
            titlePara.setSpacingBetween(1.5);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(e.getColProductSystem() != null ? e.getColProductSystem() : "产品/系统");
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            setFontStyle(titleRun);

            addField(doc, "应用角色", e.getColAppRole());
            addField(doc, "招标参数说明", e.getColBidParamDesc());
            addField(doc, "功能说明", e.getColFeatureDesc());
            addField(doc, "状态", e.getColStatus());
            addField(doc, "业务分类", e.getColBizCategory());
            addField(doc, "业务域", e.getColBizDomain());
            addField(doc, "版本划分", e.getColVersionDivision());
            addField(doc, "产品经理", e.getColProductManager());
            addField(doc, "软著", e.getColCopyright());
            addField(doc, "备注", e.getColRemark());
            updateGenRecordProgress(recordId, i + 1, entries.size());
        }
    }

    private void generateFeatureWord(XWPFDocument doc, List<DataEntry> entries, Long recordId, String docType,
                                      Boolean includeImages, Boolean compressImages) {
        int totalSize = entries.size();
        int[] progressCounter = {0};
        log.info("generateFeatureWord: total entries={}, recordId={}", totalSize, recordId);

        Set<Long> entryIds = new HashSet<>();
        Map<Long, DataEntry> entryMap = new HashMap<>();
        for (DataEntry e : entries) {
            entryIds.add(e.getId());
            entryMap.put(e.getId(), e);
        }

        LinkedHashMap<String, List<DataEntry>> categoryGroups = new LinkedHashMap<>();
        for (DataEntry e : entries) {
            String category = resolveCategory(e, entryMap);
            categoryGroups.computeIfAbsent(category, k -> new ArrayList<>()).add(e);
        }

        int categoryCounter = 0;
        for (Map.Entry<String, List<DataEntry>> catEntry : categoryGroups.entrySet()) {
            if (isCancelled(recordId)) return;
            categoryCounter++;
            String categoryText = extractText(catEntry.getKey());
            addNumberedHeading(doc, categoryText, 1, String.valueOf(categoryCounter));

            List<DataEntry> catEntries = catEntry.getValue();
            LinkedHashMap<String, List<DataEntry>> domainGroups = new LinkedHashMap<>();
            for (DataEntry e : catEntries) {
                String domain = resolveDomain(e, entryMap);
                domainGroups.computeIfAbsent(domain, k -> new ArrayList<>()).add(e);
            }

            int domainCounter = 0;
            for (Map.Entry<String, List<DataEntry>> domEntry : domainGroups.entrySet()) {
                if (isCancelled(recordId)) return;
                domainCounter++;
                String domainText = extractText(domEntry.getKey());
                String domainNumber = categoryCounter + "." + domainCounter;
                addNumberedHeading(doc, domainText, 2, domainNumber);

                List<DataEntry> domEntries = domEntry.getValue();

                Map<Long, List<DataEntry>> childrenByParent = new LinkedHashMap<>();
                List<DataEntry> roots = new ArrayList<>();
                for (DataEntry e : domEntries) {
                    if (e.getParentId() == null || e.getParentId() == 0 || !entryIds.contains(e.getParentId())) {
                        roots.add(e);
                    } else {
                        childrenByParent.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
                    }
                }
                roots.sort(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0));

                for (int i = 0; i < roots.size(); i++) {
                    if (isCancelled(recordId)) return;
                    String nodeNumber = domainNumber + "." + (i + 1);
                    writeNode(doc, roots.get(i), nodeNumber, 3, childrenByParent, recordId, progressCounter, totalSize, docType, includeImages, compressImages);
                }
            }
        }
    }

    private String resolveCategory(DataEntry e, Map<Long, DataEntry> entryMap) {
        if (e.getColBizCategory() != null && !e.getColBizCategory().trim().isEmpty()) {
            return e.getColBizCategory().trim();
        }
        if (e.getParentId() != null && e.getParentId() > 0) {
            DataEntry parent = entryMap.get(e.getParentId());
            if (parent != null) {
                String parentCat = resolveCategory(parent, entryMap);
                if (!parentCat.equals("未分类")) return parentCat;
            }
        }
        return "未分类";
    }

    private String resolveDomain(DataEntry e, Map<Long, DataEntry> entryMap) {
        if (e.getColBizDomain() != null && !e.getColBizDomain().trim().isEmpty()) {
            return e.getColBizDomain().trim();
        }
        if (e.getParentId() != null && e.getParentId() > 0) {
            DataEntry parent = entryMap.get(e.getParentId());
            if (parent != null) {
                String parentDomain = resolveDomain(parent, entryMap);
                if (!parentDomain.equals("未分类")) return parentDomain;
            }
        }
        return "未分类";
    }

    private void writeNode(XWPFDocument doc, DataEntry entry, String number, int level,
                           Map<Long, List<DataEntry>> childrenByParent,
                           Long recordId, int[] progressCounter, int totalSize, String docType, Boolean includeImages, Boolean compressImages) {
        if (isCancelled(recordId)) return;

        int docLevel = Math.min(level, MAX_HEADING_LEVEL);
        String name = extractName(entry.getColProductSystem());
        addNumberedHeading(doc, name, docLevel, number);

        String desc = "bid".equals(docType) ? entry.getColBidParamDesc() : entry.getColFeatureDesc();
        if (desc != null && !desc.isBlank()) {
            if (Boolean.TRUE.equals(includeImages)) {
                processDescriptionWithImages(doc, desc, compressImages);
            } else {
                processDescriptionTextOnly(doc, desc);
            }
        }

        progressCounter[0]++;
        updateGenRecordProgress(recordId, progressCounter[0], totalSize);

        if (isCancelled(recordId)) return;

        List<DataEntry> children = childrenByParent.get(entry.getId());
        if (children != null) {
            children.sort(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0));
            for (int i = 0; i < children.size(); i++) {
                if (isCancelled(recordId)) return;
                String childNumber = number + "." + (i + 1);
                writeNode(doc, children.get(i), childNumber, level + 1, childrenByParent, recordId, progressCounter, totalSize, docType, includeImages, compressImages);
            }
        }
    }

    String extractCode(String product) {
        if (product == null) return null;
        String text = product.trim().replace("\n", "").replace("\r", "");
        Matcher m = Pattern.compile("^[\\d.]+").matcher(text);
        return m.find() ? m.group() : null;
    }

    private void processDescriptionTextOnly(XWPFDocument doc, String description) {
        String normalized = normalizeImageCards(description);
        String cleaned = HTML_TAG_PATTERN.matcher(normalized).replaceAll("");
        cleaned = cleaned.replaceAll("\\[https?://[^\\]]*\\]", "");
        cleaned = cleaned.replaceAll("\\[local:[^\\]]*\\]", "");
        String[] lines = cleaned.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                XWPFParagraph para = doc.createParagraph();
                para.setIndentationFirstLine(420);
                para.setSpacingBetween(1.5);
                XWPFRun run = para.createRun();
                run.setText(line);
                setFontStyle(run);
            }
        }
    }

    private void processDescriptionWithImages(XWPFDocument doc, String description, Boolean compressImages) {
        String normalized = normalizeImageCards(description);
        List<String> parts = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(normalized);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                parts.add(normalized.substring(lastEnd, matcher.start()));
            }
            parts.add(matcher.group(1));
            lastEnd = matcher.end();
        }
        if (lastEnd < normalized.length()) {
            parts.add(normalized.substring(lastEnd));
        }

        int i = 0;
        while (i < parts.size()) {
            String part = parts.get(i).trim();
            if (part.isEmpty()) {
                i++;
                continue;
            }

            if (part.startsWith("http://") || part.startsWith("https://") || part.startsWith("local:")) {
                String urlForDownload = part;
                int hashIdx = urlForDownload.indexOf('#');
                if (hashIdx > 0) {
                    urlForDownload = urlForDownload.substring(0, hashIdx);
                }
                List<String> consecutiveUrls = new ArrayList<>();
                consecutiveUrls.add(urlForDownload);
                int j = i + 1;
                while (j < parts.size()) {
                    String nextPart = parts.get(j).trim();
                    if (nextPart.isEmpty()) {
                        j++;
                        continue;
                    }
                    if (nextPart.startsWith("http://") || nextPart.startsWith("https://") || nextPart.startsWith("local:")) {
                        String nextForDownload = nextPart;
                        int nextHash = nextForDownload.indexOf('#');
                        if (nextHash > 0) nextForDownload = nextForDownload.substring(0, nextHash);
                        consecutiveUrls.add(nextForDownload);
                        j++;
                    } else {
                        break;
                    }
                }

                if (consecutiveUrls.size() >= 2) {
                    List<ImageData> groupImages = new ArrayList<>();
                    for (String url : consecutiveUrls) {
                        ImageData imgData = downloadAndProcessImage(url);
                        groupImages.add(imgData);
                    }

                    List<List<String>> portraitRuns = new ArrayList<>();
                    List<String> currentRun = new ArrayList<>();
                    for (int idx = 0; idx < consecutiveUrls.size(); idx++) {
                        ImageData imgData = groupImages.get(idx);
                        boolean isPortrait = imgData != null && imgData.height > imgData.width * PORTRAIT_RATIO;
                        if (isPortrait) {
                            currentRun.add(consecutiveUrls.get(idx));
                        } else {
                            if (!currentRun.isEmpty()) {
                                portraitRuns.add(new ArrayList<>(currentRun));
                                currentRun.clear();
                            }
                            portraitRuns.add(Collections.singletonList(consecutiveUrls.get(idx)));
                        }
                    }
                    if (!currentRun.isEmpty()) {
                        portraitRuns.add(new ArrayList<>(currentRun));
                    }

                    for (List<String> run : portraitRuns) {
                        if (run.size() >= 2) {
                            List<ImageData> runImgs = new ArrayList<>();
                            for (String url : run) {
                                int ri = consecutiveUrls.indexOf(url);
                                runImgs.add(groupImages.get(ri));
                            }
                            insertImageGrid(doc, runImgs, run, compressImages);
                        } else {
                            String url = run.get(0);
                            int ri = consecutiveUrls.indexOf(url);
                            ImageData imgData = groupImages.get(ri);
                            if (imgData != null) {
                                insertSingleImage(doc, url, imgData, compressImages);
                            } else {
                                insertFallbackImage(doc);
                            }
                        }
                    }
                } else {
                    String url = consecutiveUrls.get(0);
                    ImageData imgData = downloadAndProcessImage(url);
                    if (imgData != null) {
                        insertSingleImage(doc, url, imgData, compressImages);
                    } else {
                        insertFallbackImage(doc);
                    }
                }

                i = j;
                continue;
            } else {
                String[] lines = part.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        XWPFParagraph para = doc.createParagraph();
                        para.setIndentationFirstLine(420);
                        para.setSpacingBetween(1.5);
                        XWPFRun run = para.createRun();
                        run.setText(line);
                        setFontStyle(run);
                    }
                }
                i++;
            }
        }
    }

    private static class ImageData {
        byte[] data;
        int width;
        int height;
        int pictureType;

        ImageData(byte[] data, int width, int height, int pictureType) {
            this.data = data;
            this.width = width;
            this.height = height;
            this.pictureType = pictureType;
        }
    }

    private String normalizeImageCards(String html) {
        String result = html;
        Matcher m = IMAGE_CARD_PATTERN.matcher(result);
        int cardCount = 0;
        while (m.find()) {
            int start = m.start();
            String openingTag = m.group(0);
            Matcher urlMatcher = URL_ATTR_PATTERN.matcher(openingTag);
            if (!urlMatcher.find()) continue;
            String dataUrl = urlMatcher.group(1);
            String fullUrl = dataUrl;
            if (dataUrl.startsWith("/api/images/file/")) {
                fullUrl = "local:" + dataUrl;
            }
            String filename = null;
            Matcher fnMatcher = FILENAME_ATTR_PATTERN.matcher(openingTag);
            if (fnMatcher.find()) {
                filename = fnMatcher.group(1);
            }
            String tagName = openingTag.startsWith("<div") ? "div" : "span";
            String openTagRegex = "<" + tagName + "[\\s>]";
            String closeTagStr = "</" + tagName + ">";
            int pos = m.end();
            int depth = 1;
            int contentEnd = -1;
            while (pos < result.length() && depth > 0) {
                int nextOpen = result.indexOf("<" + tagName, pos);
                if (nextOpen != -1 && nextOpen < result.length() - tagName.length() - 1) {
                    char afterOpen = result.charAt(nextOpen + 1 + tagName.length());
                    if (Character.isWhitespace(afterOpen) || afterOpen == '>') {
                        // valid opening tag
                    } else {
                        nextOpen = -1;
                    }
                }
                int nextClose = result.indexOf(closeTagStr, pos);
                if (nextClose == -1) break;
                if (nextOpen != -1 && nextOpen < nextClose) {
                    depth++;
                    pos = nextOpen + 1;
                } else {
                    depth--;
                    if (depth == 0) {
                        contentEnd = nextClose + closeTagStr.length();
                    }
                    pos = nextClose + closeTagStr.length();
                }
            }
            if (contentEnd == -1) {
                cardCount++;
                String replacement = "[" + fullUrl + "]";
                result = result.substring(0, start) + replacement + result.substring(m.end());
                m = IMAGE_CARD_PATTERN.matcher(result);
                continue;
            }
            String after = result.substring(contentEnd);
            int afterLen = after.length();
            String trimmed = after.replaceAll("^(\\s*<br\\s*/?>)+", "");
            if (trimmed.length() < afterLen) {
                contentEnd = contentEnd + (afterLen - trimmed.length());
            }
            String replacement = "[" + fullUrl;
            if (filename != null && !filename.isEmpty()) {
                replacement += "#" + filename;
            }
            replacement += "]";
            result = result.substring(0, start) + replacement + result.substring(contentEnd);
            cardCount++;
            m = IMAGE_CARD_PATTERN.matcher(result);
        }
        String cleaned = HTML_TAG_PATTERN.matcher(result).replaceAll("");
        System.out.println("[DocGen] normalizeImageCards: found " + cardCount + " cards");
        System.out.println("[DocGen] normalizeImageCards result preview: " + cleaned.substring(0, Math.min(500, cleaned.length())));
        return cleaned;
    }

    private ImageData downloadAndProcessImage(String rawUrl) {
        if (rawUrl.startsWith("local:/api/images/file/")) {
            return readLocalImage(rawUrl);
        }
        String[] candidates = buildUrlCandidates(rawUrl);
        for (String url : candidates) {
            if (url.startsWith("local:")) continue;
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8");
                conn.setRequestProperty("Referer", "https://feishu.cn/");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setInstanceFollowRedirects(true);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) continue;

                String contentType = conn.getContentType();
                if (contentType != null && contentType.startsWith("text/")) continue;

                byte[] imageData = conn.getInputStream().readAllBytes();
                if (imageData.length == 0) continue;

                return parseImageData(imageData, url);
            } catch (Exception e) {
                log.debug("URL candidate failed: {}", url);
            }
        }
        log.warn("All URL candidates failed for: {}", rawUrl);
        return null;
    }

    private ImageData readLocalImage(String rawUrl) {
        try {
            String apiPath = rawUrl.substring("local:".length());
            String relativePath = apiPath.substring("/api/images/file/".length());
            Path filePath = Paths.get(imageStoragePath, relativePath);
            if (!Files.exists(filePath)) {
                log.warn("Local image not found: {}", filePath);
                return null;
            }
            byte[] imageData = Files.readAllBytes(filePath);
            if (imageData.length == 0) return null;
            imageData = repairAndSaveTruncatedPng(filePath, imageData);
            return parseImageData(imageData, filePath.getFileName().toString());
        } catch (Exception e) {
            log.warn("Failed to read local image: {}", rawUrl, e);
            return null;
        }
    }

    private byte[] repairAndSaveTruncatedPng(Path filePath, byte[] imageData) {
        try {
            if (imageData.length < 8) return imageData;
            byte[] pngSig = {(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A};
            for (int i = 0; i < 8; i++) {
                if (imageData[i] != pngSig[i]) return imageData;
            }
            boolean hasIend = false;
            for (int i = imageData.length - PNG_IEND.length; i >= Math.max(0, imageData.length - 20); i--) {
                boolean match = true;
                for (int j = 0; j < PNG_IEND.length; j++) {
                    if (imageData[i + j] != PNG_IEND[j]) { match = false; break; }
                }
                if (match) { hasIend = true; break; }
            }
            if (!hasIend) {
                byte[] repaired = new byte[imageData.length + PNG_IEND.length];
                System.arraycopy(imageData, 0, repaired, 0, imageData.length);
                System.arraycopy(PNG_IEND, 0, repaired, imageData.length, PNG_IEND.length);
                try {
                    BufferedImage test = ImageIO.read(new ByteArrayInputStream(repaired));
                    if (test != null) {
                        Files.write(filePath, repaired);
                        log.info("Repaired truncated PNG and saved: {} ({}x{})", filePath, test.getWidth(), test.getHeight());
                        return repaired;
                    }
                } catch (Exception e) {
                    log.warn("PNG repair IEND-only failed (IDAT data truncated): {} - {}", filePath, e.getMessage());
                }
                log.warn("PNG cannot be fully repaired, will use raw bytes with IHDR dimensions: {}", filePath);
            }
        } catch (Exception e) {
            log.debug("PNG repair check failed for: {}", filePath, e);
        }
        return imageData;
    }

    private static final byte[] PNG_IEND = new byte[]{0x00,0x00,0x00,0x00, 0x49,0x45,0x4E,0x44, (byte)0xAE,0x42,0x60,(byte)0x82};

    private ImageData parseImageData(byte[] imageData, String urlOrName) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (Exception readEx) {
            log.warn("ImageIO.read threw exception for {}: {}", urlOrName, readEx.getMessage());
        }
        if (img == null) {
            img = tryRepairTruncatedPng(imageData);
        }
        if (img != null) {
            int width = img.getWidth();
            int height = img.getHeight();
            int pictureType = detectPictureType(urlOrName, img);
            return new ImageData(imageData, width, height, pictureType);
        }

        if (imageData.length > 100) {
            log.warn("Embedding raw image bytes (ImageIO unusable): {} ({} bytes)", urlOrName, imageData.length);
            String lower = urlOrName.toLowerCase();
            int pictureType;
            if (lower.contains(".jpg") || lower.contains(".jpeg")) {
                pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
            } else {
                pictureType = XWPFDocument.PICTURE_TYPE_PNG;
            }
            int[] dim = extractPngDimensions(imageData);
            return new ImageData(imageData, dim[0], dim[1], pictureType);
        }
        return null;
    }

    private BufferedImage tryRepairTruncatedPng(byte[] imageData) {
        try {
            if (imageData.length < 8) return null;
            byte[] header = new byte[]{(byte)0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A};
            for (int i = 0; i < 8; i++) {
                if (imageData[i] != header[i]) return null;
            }
            boolean hasIend = false;
            for (int i = imageData.length - PNG_IEND.length; i >= Math.max(0, imageData.length - 20); i--) {
                boolean match = true;
                for (int j = 0; j < PNG_IEND.length; j++) {
                    if (imageData[i + j] != PNG_IEND[j]) { match = false; break; }
                }
                if (match) { hasIend = true; break; }
            }
            if (!hasIend) {
                log.info("Attempting PNG repair (appending IEND): {} bytes", imageData.length);
                byte[] repaired = new byte[imageData.length + PNG_IEND.length];
                System.arraycopy(imageData, 0, repaired, 0, imageData.length);
                System.arraycopy(PNG_IEND, 0, repaired, imageData.length, PNG_IEND.length);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(repaired));
                if (img != null) {
                    log.info("PNG repair succeeded: {}x{}", img.getWidth(), img.getHeight());
                }
                return img;
            }
        } catch (Exception e) {
            log.debug("PNG repair attempt failed", e);
        }
        return null;
    }

    private int detectPictureType(String urlOrName, BufferedImage img) {
        String lower = urlOrName.toLowerCase();
        if (lower.contains(".jpg") || lower.contains(".jpeg")) {
            return XWPFDocument.PICTURE_TYPE_JPEG;
        } else if (lower.contains(".gif")) {
            return XWPFDocument.PICTURE_TYPE_GIF;
        } else if (lower.contains(".bmp")) {
            return XWPFDocument.PICTURE_TYPE_BMP;
        }
        return XWPFDocument.PICTURE_TYPE_PNG;
    }

    private int[] extractPngDimensions(byte[] imageData) {
        if (imageData.length > 24) {
            try {
                int w = ((imageData[16] & 0xFF) << 24) | ((imageData[17] & 0xFF) << 16)
                      | ((imageData[18] & 0xFF) << 8) | (imageData[19] & 0xFF);
                int h = ((imageData[20] & 0xFF) << 24) | ((imageData[21] & 0xFF) << 16)
                      | ((imageData[22] & 0xFF) << 8) | (imageData[23] & 0xFF);
                if (w > 0 && h > 0 && w <= 10000 && h <= 10000) {
                    return new int[]{w, h};
                }
            } catch (Exception ignored) {}
        }
        return new int[]{500, 300};
    }

    private byte[] compressImage(byte[] imageData, int targetWidth, int targetHeight, int pictureType) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageData));
            if (original == null) return null;
            if (original.getWidth() <= targetWidth && original.getHeight() <= targetHeight) return null;

            int newW = Math.min(targetWidth, original.getWidth());
            int newH = Math.min(targetHeight, original.getHeight());
            double ratioW = (double) targetWidth / original.getWidth();
            double ratioH = (double) targetHeight / original.getHeight();
            double ratio = Math.min(ratioW, ratioH);
            newW = (int) (original.getWidth() * ratio);
            newH = (int) (original.getHeight() * ratio);
            if (newW < 1) newW = 1;
            if (newH < 1) newH = 1;

            java.awt.Graphics2D g;
            if (pictureType == XWPFDocument.PICTURE_TYPE_PNG) {
                BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
                g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(original, 0, 0, newW, newH, null);
                g.dispose();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(scaled, "png", out);
                return out.toByteArray();
            } else {
                BufferedImage scaled = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
                g = scaled.createGraphics();
                g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, newW, newH);
                g.drawImage(original, 0, 0, newW, newH, null);
                g.dispose();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                var writers = ImageIO.getImageWritersByFormatName("jpg");
                if (writers.hasNext()) {
                    var writer = writers.next();
                    javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
                    param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(0.92f);
                    var ios = ImageIO.createImageOutputStream(out);
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(scaled, null, null), param);
                    writer.dispose();
                    ios.close();
                } else {
                    ImageIO.write(scaled, "jpg", out);
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.debug("Image compression failed, using original", e);
            return null;
        }
    }

    private String[] buildUrlCandidates(String rawUrl) {
        List<String> candidates = new ArrayList<>();
        candidates.add(rawUrl);
        try {
            String encodedUrl;
            boolean hasChineseOrSpecial = rawUrl.matches(".*[\\u4e00-\\u9fff()（）\\s].*");
            boolean hasPct = rawUrl.matches(".*%[0-9A-Fa-f]{2}.*");
            if (hasChineseOrSpecial || !hasPct) {
                int schemeEnd = rawUrl.indexOf("://");
                if (schemeEnd < 0) { return candidates.toArray(new String[0]); }
                String scheme = rawUrl.substring(0, schemeEnd);
                String rest = rawUrl.substring(schemeEnd + 3);
                int pathStart = rest.indexOf('/');
                String hostPort = pathStart >= 0 ? rest.substring(0, pathStart) : rest;
                String pathQuery = pathStart >= 0 ? rest.substring(pathStart) : "";
                String path;
                String query = "";
                int qIdx = pathQuery.indexOf('?');
                if (qIdx >= 0) {
                    path = pathQuery.substring(0, qIdx);
                    query = pathQuery.substring(qIdx);
                } else {
                    path = pathQuery;
                }
                String[] segments = path.split("/", -1);
                StringBuilder sb = new StringBuilder();
                for (String seg : segments) {
                    if (seg.isEmpty()) continue;
                    try {
                        String decoded = java.net.URLDecoder.decode(seg, "UTF-8");
                        sb.append("/").append(java.net.URLEncoder.encode(decoded, "UTF-8").replace("+", "%20"));
                    } catch (Exception e) {
                        sb.append("/").append(java.net.URLEncoder.encode(seg, "UTF-8").replace("+", "%20"));
                    }
                }
                encodedUrl = scheme + "://" + hostPort + sb + query;
            } else {
                encodedUrl = rawUrl;
            }
            if (!encodedUrl.equals(rawUrl)) {
                candidates.add(encodedUrl);
            }
        } catch (Exception ignored) {}
        return candidates.toArray(new String[0]);
    }

    private void insertSingleImage(XWPFDocument doc, String url, ImageData imgData, Boolean compressImages) {
        try {
            double targetWidthPx, targetHeightPx;
            if (imgData.height > imgData.width * PORTRAIT_RATIO) {
                targetHeightPx = IMAGE_PORTRAIT_HEIGHT_PX;
                double aspectRatio = (double) imgData.width / imgData.height;
                targetWidthPx = targetHeightPx * aspectRatio;
            } else {
                targetWidthPx = IMAGE_LANDSCAPE_WIDTH_PX;
                double aspectRatio = (double) imgData.height / imgData.width;
                targetHeightPx = targetWidthPx * aspectRatio;
            }

            int widthEMU = (int) (targetWidthPx * 9525);
            int heightEMU = (int) (targetHeightPx * 9525);

            byte[] imageDataToEmbed = imgData.data;
            int pictureTypeToEmbed = imgData.pictureType;
            if (Boolean.TRUE.equals(compressImages)) {
                byte[] compressed = compressImage(imgData.data, (int) (targetWidthPx * COMPRESS_DPI_RATIO), (int) (targetHeightPx * COMPRESS_DPI_RATIO), imgData.pictureType);
                if (compressed != null) {
                    imageDataToEmbed = compressed;
                    if (imgData.pictureType != XWPFDocument.PICTURE_TYPE_PNG) {
                        pictureTypeToEmbed = XWPFDocument.PICTURE_TYPE_JPEG;
                    }
                }
            }

            XWPFParagraph para = doc.createParagraph();
            para.setAlignment(ParagraphAlignment.CENTER);
            para.setIndentationFirstLine(420);
            XWPFRun run = para.createRun();
            run.addPicture(new ByteArrayInputStream(imageDataToEmbed), pictureTypeToEmbed,
                    "image", widthEMU, heightEMU);

            String filename = extractFilenameFromUrl(url);
            XWPFParagraph captionPara = doc.createParagraph();
            captionPara.setAlignment(ParagraphAlignment.CENTER);
            captionPara.setSpacingBetween(1.5);
            XWPFRun captionRun = captionPara.createRun();
            captionRun.setText("图：" + filename);
            captionRun.setFontSize(10);
            setFontStyle(captionRun);
            captionRun.setColor("808080");
        } catch (Exception e) {
            log.warn("Failed to insert image: {}", url, e);
            insertFallbackImage(doc);
        }
    }

    private void insertImageGrid(XWPFDocument doc, List<ImageData> images, List<String> urls, Boolean compressImages) {
        int numCols = Math.min(images.size(), 3);
        int numRows = (int) Math.ceil((double) images.size() / numCols);

        XWPFTable table = doc.createTable(numRows, numCols);
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTJcTable jc = tblPr.isSetJc() ? tblPr.getJc() : tblPr.addNewJc();
        jc.setVal(STJcTable.CENTER);
        CTTblBorders borders = tblPr.isSetTblBorders() ? tblPr.getTblBorders() : tblPr.addNewTblBorders();
        setWhiteBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop());
        setWhiteBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom());
        setWhiteBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft());
        setWhiteBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight());
        setWhiteBorder(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH());
        setWhiteBorder(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV());

        int imgIdx = 0;
        for (int r = 0; r < numRows; r++) {
            XWPFTableRow row = table.getRow(r);
            for (int c = 0; c < numCols; c++) {
                XWPFTableCell cell = row.getCell(c);
                cell.removeParagraph(0);
                XWPFParagraph cellPara = cell.addParagraph();
                cellPara.setAlignment(ParagraphAlignment.CENTER);
                CTTcPr tcPr = cell.getCTTc().addNewTcPr();
                tcPr.addNewTcW().setW(BigInteger.valueOf(3000));

                if (imgIdx < images.size()) {
                    ImageData img = images.get(imgIdx);
                    String imgUrl = urls.get(imgIdx);

                    double targetHeightPx = IMAGE_PORTRAIT_HEIGHT_PX;
                    double aspectRatio = (double) img.width / img.height;
                    double targetWidthPx = targetHeightPx * aspectRatio;

                    int widthEMU = (int) (targetWidthPx * 9525);
                    int heightEMU = (int) (targetHeightPx * 9525);

                    try {
                        byte[] dataToEmbed = img.data;
                        int pType = img.pictureType;
                        if (Boolean.TRUE.equals(compressImages)) {
                            byte[] compressed = compressImage(img.data, (int) (targetWidthPx * COMPRESS_DPI_RATIO), (int) (targetHeightPx * COMPRESS_DPI_RATIO), img.pictureType);
                            if (compressed != null) {
                                dataToEmbed = compressed;
                                if (img.pictureType != XWPFDocument.PICTURE_TYPE_PNG) {
                                    pType = XWPFDocument.PICTURE_TYPE_JPEG;
                                }
                            }
                        }
                        XWPFRun run = cellPara.createRun();
                        run.addPicture(new ByteArrayInputStream(dataToEmbed), pType,
                                "image", widthEMU, heightEMU);
                    } catch (Exception e) {
                        log.warn("Failed to insert grid image", e);
                    }

                    XWPFParagraph captionPara = cell.addParagraph();
                    captionPara.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun captionRun = captionPara.createRun();
                    captionRun.setText("图：" + extractFilenameFromUrl(imgUrl));
                    captionRun.setFontSize(8);
                    setFontStyle(captionRun);
                    captionRun.setColor("808080");

                    imgIdx++;
                }
            }
        }
    }

    private void setWhiteBorder(CTBorder border) {
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setSpace(BigInteger.ZERO);
        border.setColor("FFFFFF");
    }

    private void insertFallbackImage(XWPFDocument doc) {
        try {
            java.nio.file.Path errorPath = java.nio.file.Paths.get("uploads/error/error.png");
            if (java.nio.file.Files.exists(errorPath)) {
                byte[] errorData = java.nio.file.Files.readAllBytes(errorPath);
                BufferedImage bimg = javax.imageio.ImageIO.read(new ByteArrayInputStream(errorData));
                if (bimg != null) {
                    double targetWidthPx = 200;
                    double aspectRatio = (double) bimg.getHeight() / bimg.getWidth();
                    double targetHeightPx = targetWidthPx * aspectRatio;
                    int widthEMU = (int) (targetWidthPx * 9525);
                    int heightEMU = (int) (targetHeightPx * 9525);

                    XWPFParagraph para = doc.createParagraph();
                    para.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun run = para.createRun();
                    run.addPicture(new ByteArrayInputStream(errorData),
                            XWPFDocument.PICTURE_TYPE_PNG, "error.png", widthEMU, heightEMU);

                    XWPFParagraph captionPara = doc.createParagraph();
                    captionPara.setAlignment(ParagraphAlignment.CENTER);
                    captionPara.setSpacingBetween(1.5);
                    XWPFRun captionRun = captionPara.createRun();
                    captionRun.setText("缺失图片");
                    captionRun.setFontSize(10);
                    setFontStyle(captionRun);
                    captionRun.setColor("808080");
                    return;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load error.png fallback image", e);
        }
        XWPFParagraph para = doc.createParagraph();
        para.setAlignment(ParagraphAlignment.CENTER);
        para.setSpacingBetween(1.5);
        XWPFRun run = para.createRun();
        run.setText("缺失图片");
        setFontStyle(run);
        run.setColor("808080");
    }

    String extractText(String codeText) {
        if (codeText == null) return "";
        String text = codeText.trim();
        if (text.contains("@")) {
            text = text.split("@")[0].trim();
        }
        text = text.replaceFirst("^[\\d.]+\\s+", "");
        return text.trim();
    }

    String extractName(String product) {
        if (product == null) return "";
        String text = product.trim();
        Matcher m = Pattern.compile("^[\\d.]+\\s*(.*)").matcher(text);
        if (m.find()) {
            String name = m.group(1).trim();
            return name.isEmpty() ? text : name;
        }
        return text;
    }

    private String extractFilenameFromUrl(String url) {
        try {
            int hashIdx = url.indexOf('#');
            if (hashIdx >= 0) {
                String name = URLDecoder.decode(url.substring(hashIdx + 1), "UTF-8").trim();
                if (!name.isEmpty()) return name;
            }
            String pathToParse = url;
            if (url.startsWith("local:")) {
                pathToParse = url.substring("local:".length());
            }
            String path = pathToParse;
            int schemeEnd = pathToParse.indexOf("://");
            if (schemeEnd >= 0) {
                String rest = pathToParse.substring(schemeEnd + 3);
                int pathStart = rest.indexOf('/');
                path = pathStart >= 0 ? rest.substring(pathStart) : "";
            }
            String decoded = URLDecoder.decode(path, "UTF-8");
            String filename = decoded.substring(decoded.lastIndexOf('/') + 1);
            int queryIdx = filename.indexOf('?');
            if (queryIdx >= 0) filename = filename.substring(0, queryIdx);
            int dotIdx = filename.lastIndexOf('.');
            if (dotIdx > 0) filename = filename.substring(0, dotIdx);
            return filename.isEmpty() ? "image" : filename;
        } catch (Exception e) {
            return "image";
        }
    }

    void setFontStyle(XWPFRun run) {
        run.setFontFamily(FONT_NAME);
        run.setColor("000000");
        run.setItalic(false);
        trySetEastAsiaFont(run);
    }

    private void trySetEastAsiaFont(XWPFRun run) {
        try {
            javax.xml.namespace.QName rPrQName = new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "rPr");
            javax.xml.namespace.QName rFontsQName = new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "rFonts");
            javax.xml.namespace.QName eastAsiaQName = new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "eastAsia");
            XmlCursor cursor = run.getCTR().newCursor();
            try {
                if (cursor.toChild(rPrQName) && cursor.toChild(rFontsQName)) {
                    cursor.setAttributeText(eastAsiaQName, FONT_NAME);
                }
            } finally {
                cursor.dispose();
            }
        } catch (Exception e) {
            log.debug("Could not set eastAsia font", e);
        }
    }

    private void addNumberedHeading(XWPFDocument doc, String text, int level, String number) {
        XWPFParagraph para = doc.createParagraph();
        para.setStyle("Heading" + level);
        para.setAlignment(ParagraphAlignment.LEFT);

        CTPPr pPr = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();
        pPr.addNewOutlineLvl().setVal(BigInteger.valueOf(level - 1));

        XWPFRun run = para.createRun();
        run.setText(number + " " + text);
        setFontStyle(run);
    }

    private void ensureBuiltinHeadingStyles(XWPFDocument doc) {
        XWPFStyles styles = doc.createStyles();

        boolean allExist = true;
        for (int level = 1; level <= 9; level++) {
            if (!styles.styleExist("Heading" + level)) {
                allExist = false;
                break;
            }
        }
        if (allExist) return;

        String fontTheme = FONT_NAME;
        boolean[] headingBold = {true, true, true, true, false, false, false, false, false};
        boolean[] headingItalic = {false, false, false, true, false, true, true, false, true};
        Integer[] headingSizes = {28, 26, null, null, null, null, null, 20, 20};

        for (int level = 0; level <= 9; level++) {
            String styleId = level == 0 ? "Normal" : "Heading" + level;
            String styleName = level == 0 ? "Normal" : "heading " + level;

            CTStyle ctStyle = CTStyle.Factory.newInstance();
            ctStyle.setStyleId(styleId);
            ctStyle.setType(STStyleType.PARAGRAPH);
            if (level == 0) ctStyle.setDefault(true);
            ctStyle.addNewName().setVal(styleName);
            if (level > 0) {
                ctStyle.addNewBasedOn().setVal("Normal");
                ctStyle.addNewNext().setVal("Normal");
            }
            ctStyle.addNewUiPriority().setVal(BigInteger.valueOf(level == 0 ? 0 : 9));
            ctStyle.addNewQFormat();

            var pPr = ctStyle.addNewPPr();
            if (level > 0) {
                pPr.addNewKeepNext();
                pPr.addNewKeepLines();
                CTSpacing spacing = pPr.addNewSpacing();
                spacing.setBefore(BigInteger.valueOf(level == 1 ? 480 : 200));
                spacing.setAfter(BigInteger.valueOf(0));
                pPr.addNewOutlineLvl().setVal(BigInteger.valueOf(level - 1));
            } else {
                CTSpacing spacing = pPr.addNewSpacing();
                spacing.setLine(BigInteger.valueOf(360));
                spacing.setLineRule(STLineSpacingRule.AUTO);
            }

            var rPr = ctStyle.addNewRPr();
            CTFonts fonts = rPr.addNewRFonts();
            fonts.setAscii(fontTheme);
            fonts.setEastAsia(fontTheme);
            fonts.setHAnsi(fontTheme);
            fonts.setCs(fontTheme);
            if (level > 0 && headingBold[level - 1]) {
                rPr.addNewB();
                rPr.addNewBCs();
            }
            if (level > 0 && headingItalic[level - 1]) {
                rPr.addNewI();
                rPr.addNewICs();
            }
            if (level > 0 && headingSizes[level - 1] != null) {
                rPr.addNewSz().setVal(BigInteger.valueOf(headingSizes[level - 1]));
                rPr.addNewSzCs().setVal(BigInteger.valueOf(headingSizes[level - 1]));
            }

            XWPFStyle xwpfStyle = new XWPFStyle(ctStyle, styles);
            styles.addStyle(xwpfStyle);
        }
    }

    private void addField(XWPFDocument doc, String label, String value) {
        if (value == null || value.isEmpty()) return;
        XWPFParagraph para = doc.createParagraph();
        para.setSpacingBetween(1.5);
        XWPFRun run = para.createRun();
        run.setText(label + "：");
        run.setBold(true);
        run.setFontSize(11);
        setFontStyle(run);
        run = para.createRun();
        run.setText(value);
        run.setFontSize(11);
        setFontStyle(run);
    }

    private Long extractVersionId(List<DataEntry> entries) {
        return entries.stream()
                .filter(e -> e.getVersionId() != null)
                .map(DataEntry::getVersionId)
                .findFirst()
                .orElse(null);
    }

    private void sortByCategoryOrder(List<DataEntry> entries) {
        Long versionId = extractVersionId(entries);
        if (versionId == null) return;
        List<BaseCategory> cats = baseCategoryRepository.findByVersionIdOrderBySortOrderAsc(versionId);
        Map<String, Integer> catOrder = new LinkedHashMap<>();
        for (int i = 0; i < cats.size(); i++) catOrder.put(cats.get(i).getName(), i);
        List<BaseDomain> domains = baseDomainRepository.findByVersionId(versionId);
        domains.sort(Comparator.comparingInt(d -> d.getSortOrder() != null ? d.getSortOrder() : 0));
        Map<String, Integer> l2Order = new LinkedHashMap<>();
        for (int i = 0; i < domains.size(); i++) l2Order.put(domains.get(i).getName(), i);
        entries.sort(Comparator.comparingInt((DataEntry e) -> catOrder.getOrDefault(e.getColBizCategory(), Integer.MAX_VALUE))
                .thenComparingInt(e -> l2Order.getOrDefault(e.getColBizDomain(), Integer.MAX_VALUE))
                .thenComparingInt(e -> e.getLevel() != null ? e.getLevel() : 3)
                .thenComparing(e -> e.getParentId(), Comparator.nullsLast(Long::compareTo))
                .thenComparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
    }

    private byte[] generateExcel(String docType, List<DataEntry> entries, Long recordId) throws Exception {
        entries = entries.stream()
                .filter(e -> e.getLevel() != null && e.getLevel() >= 3)
                .collect(Collectors.toList());

        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("处理后清单");

        CellStyle headerStyle = createExcelStyle(wb, true, HorizontalAlignment.CENTER);
        CellStyle centerStyle = createExcelStyle(wb, false, HorizontalAlignment.CENTER);
        CellStyle leftStyle = createExcelStyle(wb, false, HorizontalAlignment.LEFT);

        String[] headers = {"业务分类", "业务域", "系统", "模块", "状态", "智能化", "曜", "曜最小集", "驰", "驰最小集", "远", "远最小集"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Set<Long> entryIds = new HashSet<>();
        Map<Long, DataEntry> entryMap = new HashMap<>();
        for (DataEntry e : entries) {
            entryIds.add(e.getId());
            entryMap.put(e.getId(), e);
        }

        sortByCategoryOrder(entries);

        LinkedHashMap<String, LinkedHashMap<String, List<DataEntry>>> grouped = new LinkedHashMap<>();
        for (DataEntry e : entries) {
            String cat = resolveCategory(e, entryMap);
            String dom = resolveDomain(e, entryMap);
            grouped.computeIfAbsent(cat, k -> new LinkedHashMap<>())
                    .computeIfAbsent(dom, k -> new ArrayList<>()).add(e);
        }

        int rowIdx = 1;
        int excelTotal = entries.size();
        for (Map.Entry<String, LinkedHashMap<String, List<DataEntry>>> catEntry : grouped.entrySet()) {
            String catName = extractText(catEntry.getKey());
            int catStart = rowIdx;

            for (Map.Entry<String, List<DataEntry>> domEntry : catEntry.getValue().entrySet()) {
                String domName = extractText(domEntry.getKey());
                List<DataEntry> domEntries = domEntry.getValue();
                int domStart = rowIdx;

                Map<Long, List<DataEntry>> childrenByParent = new LinkedHashMap<>();
                List<DataEntry> roots = new ArrayList<>();
                for (DataEntry e : domEntries) {
                    if (e.getParentId() == null || e.getParentId() == 0 || !entryIds.contains(e.getParentId())) {
                        roots.add(e);
                    } else {
                        childrenByParent.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
                    }
                }
                roots.sort(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0));

                for (DataEntry l3 : roots) {
                    List<DataEntry> l4List = childrenByParent.getOrDefault(l3.getId(), new ArrayList<>());
                    l4List.sort(Comparator.comparingInt(a -> a.getSortOrder() != null ? a.getSortOrder() : 0));
                    int sysStart = rowIdx;

                    if (l4List.isEmpty()) {
                        Row row = sheet.createRow(rowIdx);
                        writeExcelRow(row, catName, domName, extractName(l3.getColProductSystem()), "", l3, false, centerStyle, leftStyle);
                        rowIdx++;
                    } else {
                        for (DataEntry l4 : l4List) {
                            Row row = sheet.createRow(rowIdx);
                            boolean bubble = hasIntelligentDescendant(l4, childrenByParent);
                            writeExcelRow(row, catName, domName, extractName(l3.getColProductSystem()), extractName(l4.getColProductSystem()), l4, bubble, centerStyle, leftStyle);
                            rowIdx++;
                        }
                    }
                    updateGenRecordProgress(recordId, rowIdx - 1, excelTotal);
                    if (rowIdx - 1 > sysStart) {
                        sheet.addMergedRegion(new CellRangeAddress(sysStart, rowIdx - 1, 2, 2));
                    }
                }
                if (rowIdx - 1 > domStart) {
                    sheet.addMergedRegion(new CellRangeAddress(domStart, rowIdx - 1, 1, 1));
                }
            }
            if (rowIdx - 1 > catStart) {
                sheet.addMergedRegion(new CellRangeAddress(catStart, rowIdx - 1, 0, 0));
            }
        }

        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 18 * 256);
        sheet.setColumnWidth(2, 25 * 256);
        sheet.setColumnWidth(3, 25 * 256);
        sheet.setColumnWidth(4, 10 * 256);
        sheet.setColumnWidth(5, 8 * 256);
        sheet.setColumnWidth(6, 6 * 256);
        sheet.setColumnWidth(7, 10 * 256);
        sheet.setColumnWidth(8, 6 * 256);
        sheet.setColumnWidth(9, 10 * 256);
        sheet.setColumnWidth(10, 6 * 256);
        sheet.setColumnWidth(11, 10 * 256);

        if (recordId != null) {
            log.info("Excel数据填充完成，开始序列化: recordId={}, rows={}", recordId, excelTotal);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        if (recordId != null) {
            updateGenRecordProgress(recordId, excelTotal, excelTotal);
            log.info("Excel序列化完成: recordId={}, size={}KB", recordId, out.size() / 1024);
        }
        return out.toByteArray();
    }

    private CellStyle createExcelStyle(Workbook wb, boolean bold, HorizontalAlignment align) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(bold);
        style.setFont(font);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void writeExcelRow(Row row, String catName, String domName, String sysName, String modName,
                                DataEntry target, boolean intelligentBubble, CellStyle centerStyle, CellStyle leftStyle) {
        Cell c0 = row.createCell(0);
        c0.setCellValue(catName);
        c0.setCellStyle(centerStyle);

        Cell c1 = row.createCell(1);
        c1.setCellValue(domName);
        c1.setCellStyle(centerStyle);

        Cell c2 = row.createCell(2);
        c2.setCellValue(sysName);
        c2.setCellStyle(centerStyle);

        Cell c3 = row.createCell(3);
        c3.setCellValue(modName);
        c3.setCellStyle(leftStyle);

        Cell c4 = row.createCell(4);
        c4.setCellValue(target.getColStatus() != null ? target.getColStatus() : "");
        c4.setCellStyle(leftStyle);

        Cell c5 = row.createCell(5);
        c5.setCellValue(("1".equals(target.getColIntelligent()) || intelligentBubble) ? "是" : "");
        c5.setCellStyle(centerStyle);

        String vd = target.getColVersionDivision() != null ? target.getColVersionDivision() : "";
        boolean isYao = vd.contains("A-曜系列");
        boolean isChi = vd.contains("C-驰系列");
        boolean isYuan = vd.contains("B-远系列");

        Cell c6 = row.createCell(6);
        c6.setCellValue(isYao ? "√" : "");
        c6.setCellStyle(centerStyle);

        Cell c7 = row.createCell(7);
        if (isYao) c7.setCellValue("是".equals(target.getColYao()) ? "是" : "否");
        c7.setCellStyle(centerStyle);

        Cell c8 = row.createCell(8);
        c8.setCellValue(isChi ? "√" : "");
        c8.setCellStyle(centerStyle);

        Cell c9 = row.createCell(9);
        if (isChi) c9.setCellValue("是".equals(target.getColChi()) ? "是" : "否");
        c9.setCellStyle(centerStyle);

        Cell c10 = row.createCell(10);
        c10.setCellValue(isYuan ? "√" : "");
        c10.setCellStyle(centerStyle);

        Cell c11 = row.createCell(11);
        if (isYuan) c11.setCellValue("是".equals(target.getColYuan()) ? "是" : "否");
        c11.setCellStyle(centerStyle);
    }

    private boolean hasIntelligentDescendant(DataEntry entry, Map<Long, List<DataEntry>> childrenByParent) {
        if ("1".equals(entry.getColIntelligent())) return true;
        List<DataEntry> children = childrenByParent.getOrDefault(entry.getId(), new ArrayList<>());
        for (DataEntry child : children) {
            if (hasIntelligentDescendant(child, childrenByParent)) return true;
        }
        return false;
    }
}
