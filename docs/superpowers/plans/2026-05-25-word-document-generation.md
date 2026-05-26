# Word版功能说明文档生成 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完整复刻 generate_word.py 的逻辑在Java后端生成Word版功能说明文档，增加生成记录追踪和下载功能。

**Architecture:** 后端新增 DocGenRecord 实体记录生成历史，DocumentService 重写 generateFeatureWord 方法实现层级结构+多级编号+图片嵌入+分类分组，文档存储在服务器磁盘。前端改造生成文档弹窗，下半部分增加生成记录列表，支持异步生成和轮询状态。

**Tech Stack:** Java/Spring Boot (Apache POI), Vue 3/Element Plus, SQLite

---

### Task 1: 新增 DocGenRecord 实体和 Repository

**Files:**
- Create: `src/main/java/com/superpower/modules/document/entity/DocGenRecord.java`
- Create: `src/main/java/com/superpower/modules/document/repository/DocGenRecordRepository.java`

- [ ] **Step 1: 创建 DocGenRecord 实体**

```java
package com.superpower.modules.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "doc_gen_record")
public class DocGenRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "doc_type", length = 20)
    private String docType;

    @Column(name = "format", length = 20)
    private String format;

    @Column(length = 20)
    private String status;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_by")
    private Long generatedBy;

    @Column(name = "generated_by_name", length = 100)
    private String generatedByName;

    @Column(name = "entry_ids", length = 2000)
    private String entryIds;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
```

- [ ] **Step 2: 创建 DocGenRecordRepository**

```java
package com.superpower.modules.document.repository;

import com.superpower.modules.document.entity.DocGenRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocGenRecordRepository extends JpaRepository<DocGenRecord, Long> {
    List<DocGenRecord> findByVersionIdOrderByCreatedAtDesc(Long versionId);
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q`
Expected: BUILD SUCCESS

---

### Task 2: 重写 DocumentService - Word版功能说明生成逻辑

**Files:**
- Modify: `src/main/java/com/superpower/modules/document/service/DocumentService.java`

这是最核心的Task，需要在Java中完整复刻 generate_word.py 的逻辑：
1. 按业务分类(H1) → 业务域(H2) → 产品树(H3-H9) 层级结构
2. 多级编号 (1, 1.1, 1.1.1...)
3. 图片下载嵌入（竖图并排，横图单行）
4. 宋体、1.5倍行距、首行缩进

- [ ] **Step 1: 重写 DocumentService 完整实现**

DocumentService 需要实现以下方法：

```java
package com.superpower.modules.document.service;

import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.repository.DocGenRecordRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    private final DataEntryRepository entryRepository;
    private final DocGenRecordRepository genRecordRepository;

    @Value("${app.doc-storage-path:./generated-docs}")
    private String docStoragePath;

    public DocumentService(DataEntryRepository entryRepository, DocGenRecordRepository genRecordRepository) {
        this.entryRepository = entryRepository;
        this.genRecordRepository = genRecordRepository;
    }

    // === 生成记录管理 ===

    public DocGenRecord createGenRecord(Long versionId, String docType, String format, List<Long> entryIds, Long userId, String userName) {
        DocGenRecord record = new DocGenRecord();
        record.setVersionId(versionId);
        record.setDocType(docType);
        record.setFormat(format);
        record.setStatus("generating");
        record.setGeneratedBy(userId);
        record.setGeneratedByName(userName);
        record.setEntryIds(entryIds != null ? entryIds.stream().map(String::valueOf).collect(Collectors.joining(",")) : "");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return genRecordRepository.save(record);
    }

    public void updateGenRecordSuccess(Long recordId, String filePath, long fileSize) {
        DocGenRecord record = genRecordRepository.findById(recordId).orElse(null);
        if (record != null) {
            record.setStatus("completed");
            record.setFilePath(filePath);
            record.setFileSize(fileSize);
            record.setUpdatedAt(LocalDateTime.now());
            genRecordRepository.save(record);
        }
    }

    public void updateGenRecordError(Long recordId, String errorMessage) {
        DocGenRecord record = genRecordRepository.findById(recordId).orElse(null);
        if (record != null) {
            record.setStatus("error");
            record.setErrorMessage(errorMessage);
            record.setUpdatedAt(LocalDateTime.now());
            genRecordRepository.save(record);
        }
    }

    public List<DocGenRecord> getGenRecords(Long versionId) {
        return genRecordRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
    }

    public DocGenRecord getGenRecord(Long id) {
        return genRecordRepository.findById(id).orElse(null);
    }

    // === 文档生成主入口 ===

    public byte[] generateDocument(String docType, String format, List<Long> entryIds) throws Exception {
        List<DataEntry> entries = entryRepository.findAllById(entryIds);
        if ("word".equals(format)) {
            return generateWord(docType, entries);
        } else {
            return generateExcel(docType, entries);
        }
    }

    public String generateAndSaveDocument(Long recordId, String docType, String format, List<Long> entryIds, Long versionId) throws Exception {
        List<DataEntry> entries;
        if (entryIds == null || entryIds.isEmpty()) {
            entries = entryRepository.findByVersionIdAndLevelGreaterThanEqual(versionId, 3);
        } else {
            entries = entryRepository.findAllById(entryIds);
        }

        byte[] data;
        if ("word".equals(format)) {
            data = generateWord(docType, entries);
        } else {
            data = generateExcel(docType, entries);
        }

        Path dir = Paths.get(docStoragePath);
        Files.createDirectories(dir);
        String typeLabel = "bid".equals(docType) ? "招标参数" : "功能说明";
        String ext = "word".equals(format) ? "docx" : "xlsx";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String filename = URLEncoder.encode(typeLabel + "-" + timestamp + "." + ext, "UTF-8");
        Path filePath = dir.resolve(filename);
        Files.write(filePath, data);

        updateGenRecordSuccess(recordId, filePath.toString(), data.length);
        return filePath.toString();
    }

    // === Word 生成 ===

    private byte[] generateWord(String docType, List<DataEntry> entries) throws Exception {
        XWPFDocument doc = new XWPFDocument();

        if ("feature".equals(docType)) {
            generateFeatureWord(doc, entries);
        } else {
            generateBidWord(doc, entries);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return out.toByteArray();
    }

    private void generateFeatureWord(XWPFDocument doc, List<DataEntry> entries) {
        setDefaultLineSpacing(doc, 1.5);

        // 创建多级编号
        BigInteger numId = createMultilevelList(doc);

        // 按业务分类分组
        LinkedHashMap<String, List<DataEntry>> byCategory = new LinkedHashMap<>();
        for (DataEntry e : entries) {
            String cat = e.getColBizCategory() != null ? e.getColBizCategory() : "未分类";
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
        }

        int categoryCounter = 0;
        for (Map.Entry<String, List<DataEntry>> catEntry : byCategory.entrySet()) {
            categoryCounter++;
            String categoryText = extractText(catEntry.getKey());
            addHeadingWithStyle(doc, categoryText, 1, numId);

            // 按业务域分组
            LinkedHashMap<String, List<DataEntry>> byDomain = new LinkedHashMap<>();
            for (DataEntry e : catEntry.getValue()) {
                String domain = e.getColBizDomain() != null ? e.getColBizDomain() : "未分类";
                byDomain.computeIfAbsent(domain, k -> new ArrayList<>()).add(e);
            }

            for (Map.Entry<String, List<DataEntry>> domainEntry : byDomain.entrySet()) {
                String domainText = extractText(domainEntry.getKey());
                addHeadingWithStyle(doc, domainText, 2, numId);

                // 构建树
                List<DataEntry> domainEntries = domainEntry.getValue();
                Map<Long, DataEntry> entryMap = new HashMap<>();
                Map<Long, List<DataEntry>> childrenMap = new HashMap<>();
                List<DataEntry> roots = new ArrayList<>();

                for (DataEntry e : domainEntries) {
                    entryMap.put(e.getId(), e);
                }
                for (DataEntry e : domainEntries) {
                    if (e.getParentId() != null && entryMap.containsKey(e.getParentId())) {
                        childrenMap.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
                    } else if (e.getLevel() != null && e.getLevel() == 3) {
                        roots.add(e);
                    }
                }
                roots.sort(Comparator.comparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
                for (List<DataEntry> children : childrenMap.values()) {
                    children.sort(Comparator.comparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
                }

                int[] level3Counter = {0};
                Map<Long, String> newNumbers = new HashMap<>();
                Map<String, Integer> childCounters = new HashMap<>();

                for (DataEntry root : roots) {
                    processNodeAndChildren(doc, root, null, numId, entryMap, childrenMap, newNumbers, childCounters, level3Counter);
                }
            }
        }
    }

    private void processNodeAndChildren(XWPFDocument doc, DataEntry entry, String parentNewNum,
                                         BigInteger numId, Map<Long, DataEntry> entryMap,
                                         Map<Long, List<DataEntry>> childrenMap,
                                         Map<Long, String> newNumbers,
                                         Map<String, Integer> childCounters,
                                         int[] level3Counter) {
        String currentNewNum;
        if (entry.getLevel() != null && entry.getLevel() == 3) {
            level3Counter[0]++;
            currentNewNum = String.valueOf(level3Counter[0]);
        } else {
            String parentKey = parentNewNum != null ? parentNewNum : "root";
            int counter = childCounters.getOrDefault(parentKey, 0) + 1;
            childCounters.put(parentKey, counter);
            currentNewNum = parentNewNum + "." + counter;
        }
        newNumbers.put(entry.getId(), currentNewNum);

        String name = entry.getColProductSystem() != null ? entry.getColProductSystem() : "(无名称)";
        int docLevel = currentNewNum.split("\\.").length + 2;
        docLevel = Math.min(Math.max(docLevel, 3), 9);

        addHeadingWithStyle(doc, name, docLevel, numId);

        String description = entry.getColFeatureDesc();
        if (description != null && !description.isBlank()) {
            processDescriptionWithImages(doc, description);
        }

        List<DataEntry> children = childrenMap.getOrDefault(entry.getId(), Collections.emptyList());
        for (DataEntry child : children) {
            processNodeAndChildren(doc, child, currentNewNum, numId, entryMap, childrenMap, newNumbers, childCounters, level3Counter);
        }
    }

    // === 图片处理 ===

    private void processDescriptionWithImages(XWPFDocument doc, String description) {
        Pattern urlPattern = Pattern.compile("<([^<>]+)>");
        Matcher matcher = urlPattern.matcher(description);
        List<String> parts = new ArrayList<>();
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                parts.add(description.substring(lastEnd, matcher.start()));
            }
            parts.add(matcher.group(1));
            lastEnd = matcher.end();
        }
        if (lastEnd < description.length()) {
            parts.add(description.substring(lastEnd));
        }

        int i = 0;
        while (i < parts.size()) {
            String part = parts.get(i).trim();
            if (part.isEmpty()) { i++; continue; }

            if (part.startsWith("http://") || part.startsWith("https://")) {
                // 收集连续URL
                List<String> consecutiveUrls = new ArrayList<>();
                consecutiveUrls.add(part);
                int j = i + 1;
                while (j < parts.size()) {
                    String next = parts.get(j).trim();
                    if (next.isEmpty()) { j++; continue; }
                    if (next.startsWith("http://") || next.startsWith("https://")) {
                        consecutiveUrls.add(next);
                        j++;
                    } else {
                        break;
                    }
                }

                if (consecutiveUrls.size() >= 2) {
                    // 检查是否为竖图组
                    List<ImageData> groupImages = new ArrayList<>();
                    boolean isVerticalGroup = false;
                    for (String url : consecutiveUrls) {
                        ImageData imgData = downloadAndProcessImage(url);
                        if (imgData == null) break;
                        groupImages.add(imgData);
                        if (imgData.height > imgData.width * 1.2) isVerticalGroup = true;
                    }

                    if (isVerticalGroup && groupImages.size() >= 2) {
                        insertImageGrid(doc, groupImages);
                    } else {
                        for (ImageData imgData : groupImages) {
                            insertSingleImage(doc, imgData);
                        }
                        for (int k = groupImages.size(); k < consecutiveUrls.size(); k++) {
                            insertFallbackText(doc, consecutiveUrls.get(k));
                        }
                    }
                } else {
                    ImageData imgData = downloadAndProcessImage(consecutiveUrls.get(0));
                    if (imgData != null) {
                        insertSingleImage(doc, imgData);
                    } else {
                        insertFallbackText(doc, consecutiveUrls.get(0));
                    }
                }
                i = j;
                continue;
            } else {
                // 纯文本
                for (String line : part.split("\n")) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        XWPFParagraph para = doc.createParagraph();
                        para.setIndentationFirstLine(BigInteger.valueOf(420));
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
        String url;
        byte[] data;
        int width;
        int height;
        String filename;

        ImageData(String url, byte[] data, int width, int height, String filename) {
            this.url = url; this.data = data; this.width = width;
            this.height = height; this.filename = filename;
        }
    }

    private ImageData downloadAndProcessImage(String url) {
        try {
            String fixedUrl = fixImageUrl(url);
            HttpURLConnection conn = (HttpURLConnection) new URL(fixedUrl).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Referer", "https://feishu.cn/");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) return null;

            String contentType = conn.getContentType();
            if (contentType != null && contentType.startsWith("text/")) return null;

            byte[] data = conn.getInputStream().readAllBytes();
            conn.disconnect();
            if (data.length == 0) return null;

            // 尝试读取尺寸
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
            if (img == null) return null;

            String path = new URL(fixedUrl).getPath();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            if (filename.contains("?")) filename = filename.substring(0, filename.indexOf("?"));
            filename = filename.replaceAll("\\.\\w+$", "");

            return new ImageData(url, data, img.getWidth(), img.getHeight(), filename);
        } catch (Exception e) {
            return null;
        }
    }

    private void insertSingleImage(XWPFDocument doc, ImageData imgData) {
        try {
            int targetWidth, targetHeight;
            if (imgData.height > imgData.width * 1.2) {
                targetHeight = 300;
                targetWidth = (int) (targetHeight * ((double) imgData.width / imgData.height));
            } else {
                targetWidth = 500;
                targetHeight = (int) (targetWidth * ((double) imgData.height / imgData.width));
            }

            XWPFParagraph para = doc.createParagraph();
            para.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run = para.createRun();
            run.addPicture(new ByteArrayInputStream(imgData.data),
                    XWPFDocument.PICTURE_TYPE_PNG, imgData.filename + ".png",
                    Units.toEMU(targetWidth), Units.toEMU(targetHeight));

            XWPFParagraph captionPara = doc.createParagraph();
            captionPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun captionRun = captionPara.createRun();
            captionRun.setText("图：" + imgData.filename);
            captionRun.setFontSize(10);
            captionRun.setColor("808080");
        } catch (Exception e) {
            insertFallbackText(doc, imgData.url);
        }
    }

    private void insertImageGrid(XWPFDocument doc, List<ImageData> images) {
        int numCols = Math.min(images.size(), 3);
        int numRows = (int) Math.ceil((double) images.size() / numCols);

        XWPFTable table = doc.createTable(numRows, numCols);
        // 隐藏边框
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblBorders borders = tblPr.addNewTblBorders();
        setBorderNone(borders.addNewTop());
        setBorderNone(borders.addNewLeft());
        setBorderNone(borders.addNewBottom());
        setBorderNone(borders.addNewRight());
        setBorderNone(borders.addNewInsideH());
        setBorderNone(borders.addNewInsideV());

        for (int ri = 0; ri < numRows; ri++) {
            XWPFTableRow row = table.getRow(ri);
            for (int ci = 0; ci < numCols; ci++) {
                int imgIdx = ri * numCols + ci;
                XWPFTableCell cell = row.getCell(ci);
                // 隐藏单元格边框
                CTTcPr tcPr = cell.getCTTc().addNewTcPr();
                CTTcBorders tcBorders = tcPr.addNewTcBdr();
                setBorderNone(tcBorders.addNewTop());
                setBorderNone(tcBorders.addNewLeft());
                setBorderNone(tcBorders.addNewBottom());
                setBorderNone(tcBorders.addNewRight());

                if (imgIdx < images.size()) {
                    ImageData imgData = images.get(imgIdx);
                    try {
                        int targetHeight = 300;
                        int targetWidth = (int) (targetHeight * ((double) imgData.width / imgData.height));

                        XWPFParagraph para = cell.getParagraphs().get(0);
                        para.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun run = para.createRun();
                        run.addPicture(new ByteArrayInputStream(imgData.data),
                                XWPFDocument.PICTURE_TYPE_PNG, imgData.filename + ".png",
                                Units.toEMU(targetWidth), Units.toEMU(targetHeight));

                        XWPFParagraph captionPara = cell.addParagraph();
                        captionPara.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun captionRun = captionPara.createRun();
                        captionRun.setText("图：" + imgData.filename);
                        captionRun.setFontSize(8);
                        captionRun.setColor("808080");
                    } catch (Exception e) {
                        cell.getText();
                        cell.removeParagraph(0);
                        cell.addParagraph().createRun().setText(imgData.url);
                    }
                }
            }
        }
    }

    private void setBorderNone(CTBorder border) {
        border.setVal(STBorder.NONE);
        border.setSz(BigInteger.ZERO);
        border.setSpace(BigInteger.ZERO);
        border.setColor("FFFFFF");
    }

    private void insertFallbackText(XWPFDocument doc, String url) {
        XWPFParagraph para = doc.createParagraph();
        para.setIndentationFirstLine(BigInteger.valueOf(420));
        XWPFRun run = para.createRun();
        run.setText("<" + url + ">");
        setFontStyle(run);
    }

    // === 样式和编号 ===

    private void setDefaultLineSpacing(XWPFDocument doc, double spacing) {
        XWPFStyles styles = doc.createStyles();
        XWPFStyle normalStyle = styles.getStyle("Normal");
        if (normalStyle == null) {
            normalStyle = new XWPFStyle(new CTStyle());
            normalStyle.getStyle().setStyleId("Normal");
        }
        CTSpacing ctSpacing = normalStyle.getCTStyle().addNewPPr().addNewSpacing();
        ctSpacing.setLine(BigInteger.valueOf((long)(spacing * 240)));
        styles.addStyle(normalStyle);
    }

    private void setFontStyle(XWPFRun run) {
        run.setFontFamily("宋体");
        run.setColor("000000");
        run.setItalic(false);
    }

    private void addHeadingWithStyle(XWPFDocument doc, String text, int level, BigInteger numId) {
        XWPFParagraph para = doc.createParagraph();
        String styleName = "Heading" + level;
        para.setStyle(styleName);

        // 添加多级编号
        CTNumPr numPr = para.getCTP().addNewPPr().addNewNumPr();
        numPr.addNewIlvl().setVal(BigInteger.valueOf(level - 1));
        numPr.addNewNumId().setVal(numId);

        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontFamily("宋体");
        run.setColor("000000");
        run.setItalic(false);
        para.setAlignment(ParagraphAlignment.LEFT);
    }

    private BigInteger createMultilevelList(XWPFDocument doc) {
        CTNumbering numbering = doc.getDocument().getNumbering();
        if (numbering == null) {
            numbering = doc.getDocument().addNewNumbering();
        }

        CTAbstractNum abstractNum = numbering.addNewAbstractNum();
        BigInteger abstractNumId = BigInteger.valueOf(numbering.sizeOfAbstractNumArray() - 1);
        abstractNum.setAbstractNumId(abstractNumId);
        abstractNum.setMultiLevelType(CTMultiLevelType.HYBRID_MULTILEVEL);

        String[][] levelFormats = {
            {"decimal", "%1"}, {"decimal", "%1.%2"}, {"decimal", "%1.%2.%3"},
            {"decimal", "%1.%2.%3.%4"}, {"decimal", "%1.%2.%3.%4.%5"},
            {"decimal", "%1.%2.%3.%4.%5.%6"}, {"decimal", "%1.%2.%3.%4.%5.%6.%7"},
            {"decimal", "%1.%2.%3.%4.%5.%6.%7.%8"}, {"decimal", "%1.%2.%3.%4.%5.%6.%7.%8.%9"}
        };

        for (int lvl = 0; lvl < 9; lvl++) {
            CTLvl ctLvl = abstractNum.addNewLvl();
            ctLvl.setIlvl(BigInteger.valueOf(lvl));
            ctLvl.addNewStart().setVal(BigInteger.ONE);
            ctLvl.addNewNumFmt().setVal(STNumberFormat.DECIMAL);
            ctLvl.addNewLvlText().setVal(levelFormats[lvl][1]);
            ctLvl.addNewSuff().setVal(STLevelSuffix.SPACE);
            ctLvl.addNewLvlJc().setVal(STJc.LEFT);
        }

        CTNum num = numbering.addNewNum();
        BigInteger numId = BigInteger.valueOf(numbering.sizeOfNumArray() - 1);
        num.setNumId(numId);
        num.addNewAbstractNumId().setVal(abstractNumId);

        return numId;
    }

    private String extractText(String text) {
        if (text == null) return "";
        text = text.trim();
        if (text.contains("@")) text = text.split("@")[0].trim();
        text = text.replaceAll("^[\\d\\.]+\\s+", "");
        return text.trim();
    }

    private String fixImageUrl(String url) {
        url = url.replaceAll("(\\d)(png|jpg|jpeg|gif|bmp|webp)$", "$1.$2");
        return url;
    }

    // === 招标参数 Word ===

    private void generateBidWord(XWPFDocument doc, List<DataEntry> entries) {
        for (DataEntry e : entries) {
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
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
        }
    }

    private void addField(XWPFDocument doc, String label, String value) {
        if (value == null || value.isEmpty()) return;
        XWPFParagraph para = doc.createParagraph();
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

    // === Excel 生成 ===

    private byte[] generateExcel(String docType, List<DataEntry> entries) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet sheet;

        if ("bid".equals(docType)) {
            sheet = wb.createSheet("招标参数");
            String[] headers = {"产品/系统", "应用角色", "状态", "业务分类", "业务域", "版本划分", "产品经理", "招标参数说明", "功能说明", "软著"};
            String[] fields = {"colProductSystem", "colAppRole", "colStatus", "colBizCategory", "colBizDomain",
                    "colVersionDivision", "colProductManager", "colBidParamDesc", "colFeatureDesc", "colCopyright"};
            fillSheet(sheet, headers, fields, entries);
        } else {
            sheet = wb.createSheet("功能说明");
            String[] headers = {"产品/系统", "应用角色", "状态", "业务分类", "业务域", "产品经理", "功能说明", "招标参数说明"};
            String[] fields = {"colProductSystem", "colAppRole", "colStatus", "colBizCategory", "colBizDomain",
                    "colProductManager", "colFeatureDesc", "colBidParamDesc"};
            fillSheet(sheet, headers, fields, entries);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();
        return out.toByteArray();
    }

    private void fillSheet(Sheet sheet, String[] headers, String[] fields, List<DataEntry> entries) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }
        for (int r = 0; r < entries.size(); r++) {
            Row row = sheet.createRow(r + 1);
            DataEntry e = entries.get(r);
            for (int c = 0; c < fields.length; c++) {
                String val = getFieldValue(e, fields[c]);
                row.createCell(c).setCellValue(val != null ? val : "");
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String getFieldValue(DataEntry e, String field) {
        return switch (field) {
            case "colProductSystem" -> e.getColProductSystem();
            case "colAppRole" -> e.getColAppRole();
            case "colStatus" -> e.getColStatus();
            case "colBizCategory" -> e.getColBizCategory();
            case "colBizDomain" -> e.getColBizDomain();
            case "colVersionDivision" -> e.getColVersionDivision();
            case "colProductManager" -> e.getColProductManager();
            case "colBidParamDesc" -> e.getColBidParamDesc();
            case "colFeatureDesc" -> e.getColFeatureDesc();
            case "colCopyright" -> e.getColCopyright();
            default -> "";
        };
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q 2>&1 | tail -10`
Expected: BUILD SUCCESS (可能需要调整import和CT*类的用法)

---

### Task 3: 改造 DocumentController - 支持异步生成和记录查询

**Files:**
- Modify: `src/main/java/com/superpower/modules/document/controller/DocumentController.java`
- Modify: `src/main/java/com/superpower/modules/document/dto/DocGenerateRequest.java`

- [ ] **Step 1: 更新 DocGenerateRequest 增加 versionId 和 dataScope**

```java
package com.superpower.modules.document.dto;

import lombok.Data;
import java.util.List;

@Data
public class DocGenerateRequest {
    private Long versionId;
    private String docType;
    private String format;
    private String dataScope;
    private List<Long> entryIds;
}
```

- [ ] **Step 2: 重写 DocumentController**

```java
package com.superpower.modules.document.controller;

import com.superpower.common.Result;
import com.superpower.modules.document.dto.DocGenerateRequest;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.service.DocumentService;
import com.superpower.modules.system.entity.SysUser;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/generate")
    public Result<DocGenRecord> generate(@Valid @RequestBody DocGenerateRequest request, Authentication auth) {
        Long userId = null;
        String userName = "";
        if (auth != null && auth.getPrincipal() instanceof SysUser user) {
            userId = user.getId();
            userName = user.getNickname() != null ? user.getNickname() : user.getUsername();
        }

        List<Long> entryIds = "all".equals(request.getDataScope()) ? List.of() : request.getEntryIds();

        DocGenRecord record = documentService.createGenRecord(
                request.getVersionId(), request.getDocType(), request.getFormat(), entryIds, userId, userName);

        Long recordId = record.getId();
        new Thread(() -> {
            try {
                documentService.generateAndSaveDocument(
                        recordId, request.getDocType(), request.getFormat(), entryIds, request.getVersionId());
            } catch (Exception e) {
                documentService.updateGenRecordError(recordId, e.getMessage());
            }
        }).start();

        return Result.success(record);
    }

    @GetMapping("/records")
    public Result<List<DocGenRecord>> getRecords(@RequestParam Long versionId) {
        return Result.success(documentService.getGenRecords(versionId));
    }

    @GetMapping("/records/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        DocGenRecord record = documentService.getGenRecord(id);
        if (record == null || !"completed".equals(record.getStatus())) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(record.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        String filename = URLEncoder.encode(file.getName(), StandardCharsets.UTF_8);
        String contentType = record.getFormat().equals("word")
                ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .body(new FileSystemResource(file));
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn compile -q 2>&1 | tail -10`
Expected: BUILD SUCCESS

---

### Task 4: 配置文件存储路径

**Files:**
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: 添加文档存储路径配置**

在 `app:` 节点下增加：

```yaml
app:
  jwt:
    secret: superpower-test-secret-key-for-jwt-token-generation-2026
    expiration-ms: 86400000
  doc-storage-path: ./generated-docs
```

---

### Task 5: 前端 API 层 - 增加记录查询和下载接口

**Files:**
- Modify: `frontend/src/api/document.js`

- [ ] **Step 1: 增加记录查询和下载API**

```javascript
import request from '../utils/request'

export function generateDocument(data) {
  return request.post('/documents/generate', data)
}

export function getDocRecords(versionId) {
  return request.get('/documents/records', { params: { versionId } })
}

export function downloadDocument(recordId) {
  return request.get(`/documents/records/${recordId}/download`, {
    responseType: 'blob'
  })
}
```

注意：`generateDocument` 不再使用 `responseType: 'blob'`，改为返回 `DocGenRecord` 对象。

---

### Task 6: 前端 - 改造生成文档弹窗

**Files:**
- Modify: `frontend/src/views/dashboard/DataWorkbench.vue`

- [ ] **Step 1: 重写弹窗模板，增加数据范围选择和生成记录列表**

将 `<el-dialog>` 部分替换为：

```html
<el-dialog v-model="showDocDialog" title="生成文档" width="700px">
  <el-form label-width="100px" size="small">
    <el-form-item label="文档类型">
      <el-radio-group v-model="docType">
        <el-radio value="bid">招标参数</el-radio>
        <el-radio value="feature">功能说明</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="输出格式">
      <el-radio-group v-model="docFormat">
        <el-radio value="word">Word (.docx)</el-radio>
        <el-radio value="excel">Excel (.xlsx)</el-radio>
      </el-radio-group>
    </el-form-item>
    <el-form-item label="数据范围">
      <el-radio-group v-model="dataScope">
        <el-radio value="selected">仅勾选产品 ({{ selectedEntryIds.length }}条)</el-radio>
        <el-radio value="all">整个版本</el-radio>
      </el-radio-group>
    </el-form-item>
  </el-form>

  <el-divider content-position="left">生成记录</el-divider>
  <el-table :data="genRecords" size="small" max-height="300" v-loading="recordsLoading">
    <el-table-column label="状态" width="90">
      <template #default="{ row }">
        <el-tag v-if="row.status === 'generating'" type="warning" size="small">生成中</el-tag>
        <el-tag v-else-if="row.status === 'completed'" type="success" size="small">已完成</el-tag>
        <el-tag v-else-if="row.status === 'error'" type="danger" size="small">生成错误</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="文档类型" width="140">
      <template #default="{ row }">
        {{ row.format === 'word' ? 'Word' : 'Excel' }}版{{ row.docType === 'bid' ? '招标参数' : '功能说明' }}
      </template>
    </el-table-column>
    <el-table-column label="生成时间" width="160">
      <template #default="{ row }">
        {{ row.createdAt ? row.createdAt.replace('T', ' ').substring(0, 19) : '' }}
      </template>
    </el-table-column>
    <el-table-column label="文档大小" width="90">
      <template #default="{ row }">
        {{ row.fileSize ? (row.fileSize / 1024).toFixed(1) + 'KB' : '-' }}
      </template>
    </el-table-column>
    <el-table-column prop="generatedByName" label="生成人" width="80" />
    <el-table-column label="操作" width="80">
      <template #default="{ row }">
        <el-button v-if="row.status === 'completed'" type="primary" link size="small"
                   @click="handleDownload(row)">下载</el-button>
      </template>
    </el-table-column>
  </el-table>

  <template #footer>
    <el-button @click="showDocDialog = false">取消</el-button>
    <el-button type="primary" :loading="docLoading" @click="handleGenerate">生成</el-button>
  </template>
</el-dialog>
```

- [ ] **Step 2: 更新 script 部分**

```javascript
import { ref, onMounted, watch } from 'vue'
import TreePanel from '../../components/TreePanel.vue'
import StatsTab from '../../components/StatsTab.vue'
import DataListTab from '../../components/DataListTab.vue'
import { generateDocument, getDocRecords, downloadDocument } from '../../api/document'
import { getVersions } from '../../api/version'
import { ElMessage } from 'element-plus'

const versions = ref([])
const selectedVersion = ref(null)
const showVersionDialog = ref(false)
const selectedNode = ref(null)
const activeTab = ref('stats')
const showDocDialog = ref(false)
const docType = ref('feature')
const docFormat = ref('word')
const dataScope = ref('selected')
const selectedEntryIds = ref([])
const docLoading = ref(false)
const genRecords = ref([])
const recordsLoading = ref(false)
let pollTimer = null

onMounted(async () => {
  const res = await getVersions()
  versions.value = res.data || []
})

watch(showDocDialog, (val) => {
  if (val && selectedVersion.value) {
    loadGenRecords()
  } else {
    stopPolling()
  }
})

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

function onVersionSelect(version) {
  selectedVersion.value = version
  showVersionDialog.value = false
  selectedNode.value = null
}

function onTreeSelect(node) {
  selectedNode.value = node
}

function onGenerateDoc(ids) {
  selectedEntryIds.value = ids
  showDocDialog.value = true
}

async function loadGenRecords() {
  if (!selectedVersion.value) return
  recordsLoading.value = true
  try {
    const res = await getDocRecords(selectedVersion.value.id)
    genRecords.value = res.data || []
  } finally {
    recordsLoading.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => {
    loadGenRecords()
    const hasGenerating = genRecords.value.some(r => r.status === 'generating')
    if (!hasGenerating) stopPolling()
  }, 2000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

async function handleGenerate() {
  if (dataScope.value === 'selected' && selectedEntryIds.value.length === 0) {
    ElMessage.warning('请先选择数据')
    return
  }
  docLoading.value = true
  try {
    const res = await generateDocument({
      versionId: selectedVersion.value.id,
      docType: docType.value,
      format: docFormat.value,
      dataScope: dataScope.value,
      entryIds: dataScope.value === 'selected' ? selectedEntryIds.value : []
    })
    if (res.code === 200) {
      ElMessage.success('文档正在生成中...')
      loadGenRecords()
      startPolling()
    } else {
      ElMessage.error(res.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('文档生成请求失败')
  } finally {
    docLoading.value = false
  }
}

async function handleDownload(row) {
  try {
    const res = await downloadDocument(row.id)
    const ext = row.format === 'word' ? 'docx' : 'xlsx'
    const label = row.docType === 'bid' ? '招标参数' : '功能说明'
    const blob = new Blob([res], {
      type: row.format === 'word'
        ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${label}.${ext}`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('下载失败')
  }
}
```

- [ ] **Step 3: 构建前端验证**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest/frontend && npm run build 2>&1 | grep -E "✓|error" | head -5`
Expected: ✓ built in Xs

---

### Task 7: 端到端测试和修复

- [ ] **Step 1: 重启后端**

Run: `cd /Users/craneking/workspace/工程设计/superPowerTest && mvn spring-boot:run`

- [ ] **Step 2: 在浏览器中测试完整流程**
1. 打开 http://localhost:5173/
2. 选择版本，进入数据清单
3. 勾选几个产品，点击"生成文档"
4. 选择"功能说明" + "Word (.docx)" + "仅勾选产品"
5. 点击生成，观察生成记录列表中出现"生成中"状态
6. 等待状态变为"已完成"
7. 点击下载，验证Word文档是否包含层级结构、编号和图片

- [ ] **Step 3: 修复编译和运行时错误**

根据Step 2的结果修复问题。重点关注：
- Apache POI 的 CT* 类导入是否正确
- 多级编号 XML 是否正确生成
- 图片下载和嵌入是否工作
- 文件存储路径是否可写

---

### Task 8: 安全配置 - 允许生成记录接口匿名访问（如果需要）

**Files:**
- Check: `src/main/java/com/superpower/config/SecurityConfig.java`

- [ ] **Step 1: 检查 SecurityConfig 确保文档API需要认证**

确认 `/api/documents/**` 不在 permitAll 列表中即可。文档生成和下载都需要登录。
