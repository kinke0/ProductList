package com.superpower.modules.document.service;

import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.document.entity.DocGenRecord;
import com.superpower.modules.document.repository.DocGenRecordRepository;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private static final Pattern URL_PATTERN = Pattern.compile("<([^<>]+)>");

    private final DataEntryRepository entryRepository;
    private final DocGenRecordRepository genRecordRepository;

    @Value("${app.doc-storage-path:./generated-docs}")
    private String docStoragePath;

    public DocumentService(DataEntryRepository entryRepository, DocGenRecordRepository genRecordRepository) {
        this.entryRepository = entryRepository;
        this.genRecordRepository = genRecordRepository;
    }

    public DocGenRecord createGenRecord(Long versionId, String docType, String format,
                                        List<Long> entryIds, Long userId, String userName) {
        DocGenRecord record = new DocGenRecord();
        record.setVersionId(versionId);
        record.setDocType(docType);
        record.setFormat(format);
        record.setEntryIds(entryIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        record.setGeneratedBy(userId);
        record.setGeneratedByName(userName);
        record.setStatus("generating");
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        return genRecordRepository.save(record);
    }

    public void updateGenRecordSuccess(Long recordId, String filePath, long fileSize) {
        DocGenRecord record = genRecordRepository.findById(recordId).orElseThrow();
        record.setFilePath(filePath);
        record.setFileSize(fileSize);
        record.setStatus("completed");
        record.setUpdatedAt(LocalDateTime.now());
        genRecordRepository.save(record);
    }

    public void updateGenRecordError(Long recordId, String errorMessage) {
        DocGenRecord record = genRecordRepository.findById(recordId).orElseThrow();
        record.setErrorMessage(errorMessage);
        record.setStatus("error");
        record.setUpdatedAt(LocalDateTime.now());
        genRecordRepository.save(record);
    }

    public List<DocGenRecord> getGenRecords(Long versionId) {
        return genRecordRepository.findByVersionIdOrderByCreatedAtDesc(versionId);
    }

    public DocGenRecord getGenRecord(Long id) {
        return genRecordRepository.findById(id).orElse(null);
    }

    public void deleteGenRecord(Long id) {
        DocGenRecord record = genRecordRepository.findById(id).orElse(null);
        if (record != null) {
            if (record.getFilePath() != null) {
                try { java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(record.getFilePath())); } catch (Exception ignored) {}
            }
            genRecordRepository.deleteById(id);
        }
    }

    public byte[] generateDocument(String docType, String format, List<Long> entryIds) throws Exception {
        List<DataEntry> entries = entryRepository.findAllById(entryIds);
        if ("word".equals(format)) {
            return generateWord(docType, entries);
        } else {
            return generateExcel(docType, entries);
        }
    }

    public String generateAndSaveDocument(Long recordId, String docType, String format,
                                          List<Long> entryIds, Long versionId) throws Exception {
        List<DataEntry> entries;
        if (entryIds == null || entryIds.isEmpty()) {
            entries = entryRepository.findByVersionId(versionId);
        } else {
            entries = new ArrayList<>(entryRepository.findAllById(entryIds));
            List<Long> allIds = new ArrayList<>(entries.stream().map(DataEntry::getId).toList());
            List<DataEntry> children = entryRepository.findByVersionId(versionId);
            collectDescendants(allIds, children);
            entries = entryRepository.findAllById(allIds);
        }
        byte[] data;
        if ("word".equals(format)) {
            data = generateWord(docType, entries);
        } else {
            data = generateExcel(docType, entries);
        }

        Path dir = Paths.get(docStoragePath);
        Files.createDirectories(dir);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String ext = "word".equals(format) ? ".docx" : ".xlsx";
        String filename = docType + "_" + timestamp + "_" + recordId + ext;
        Path filePath = dir.resolve(filename);
        Files.write(filePath, data);

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

    private byte[] generateWord(String docType, List<DataEntry> entries) throws Exception {
        XWPFDocument doc = new XWPFDocument();
        ensureBuiltinHeadingStyles(doc);
        if ("bid".equals(docType)) {
            generateBidWord(doc, entries);
        } else {
            generateFeatureWord(doc, entries);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.write(out);
        doc.close();
        return out.toByteArray();
    }

    private void generateBidWord(XWPFDocument doc, List<DataEntry> entries) {
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
        }
    }

    private void generateFeatureWord(XWPFDocument doc, List<DataEntry> entries) {
        BigInteger numId = initMultilevelNumbering(doc);

        LinkedHashMap<String, List<DataEntry>> categoryGroups = new LinkedHashMap<>();
        for (DataEntry e : entries) {
            String category = e.getColBizCategory() != null ? e.getColBizCategory() : "";
            categoryGroups.computeIfAbsent(category, k -> new ArrayList<>()).add(e);
        }

        for (Map.Entry<String, List<DataEntry>> catEntry : categoryGroups.entrySet()) {
            String categoryText = extractText(catEntry.getKey());
            addNumberedHeading(doc, categoryText, 1, numId);

            List<DataEntry> catEntries = catEntry.getValue();
            LinkedHashMap<String, List<DataEntry>> domainGroups = new LinkedHashMap<>();
            for (DataEntry e : catEntries) {
                String domain = e.getColBizDomain() != null ? e.getColBizDomain() : "";
                domainGroups.computeIfAbsent(domain, k -> new ArrayList<>()).add(e);
            }

            for (Map.Entry<String, List<DataEntry>> domEntry : domainGroups.entrySet()) {
                String domainText = extractText(domEntry.getKey());
                addNumberedHeading(doc, domainText, 2, numId);

                List<DataEntry> domEntries = domEntry.getValue();
                List<DataEntry> treeEntries = domEntries.stream()
                        .filter(e -> e.getLevel() != null && e.getLevel() >= 3)
                        .sorted(Comparator.comparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0))
                        .collect(Collectors.toList());

                if (treeEntries.isEmpty()) continue;

                Set<Long> treeIds = treeEntries.stream().map(DataEntry::getId).collect(Collectors.toSet());
                Map<Long, List<DataEntry>> childrenMap = new LinkedHashMap<>();
                List<DataEntry> rootEntries = new ArrayList<>();

                for (DataEntry e : treeEntries) {
                    if (e.getParentId() == null || !treeIds.contains(e.getParentId())) {
                        rootEntries.add(e);
                    } else {
                        childrenMap.computeIfAbsent(e.getParentId(), k -> new ArrayList<>()).add(e);
                    }
                }

                for (List<DataEntry> children : childrenMap.values()) {
                    children.sort(Comparator.comparingInt(e -> e.getSortOrder() != null ? e.getSortOrder() : 0));
                }

                int[] level3Counter = {0};
                Map<String, Integer> childCounters = new HashMap<>();
                for (DataEntry root : rootEntries) {
                    processNodeAndChildren(doc, root, "", childrenMap, numId, level3Counter, childCounters);
                }
            }
        }
    }

    private void processNodeAndChildren(XWPFDocument doc, DataEntry entry,
                                         String parentNewNum, Map<Long, List<DataEntry>> childrenMap,
                                         BigInteger numId, int[] level3Counter,
                                         Map<String, Integer> childCounters) {
        String currentNewNum;
        if (entry.getLevel() != null && entry.getLevel() == 3) {
            level3Counter[0]++;
            currentNewNum = String.valueOf(level3Counter[0]);
        } else {
            String parentKey = parentNewNum != null && !parentNewNum.isEmpty() ? parentNewNum : "0";
            int counter = childCounters.getOrDefault(parentKey, 0) + 1;
            childCounters.put(parentKey, counter);
            currentNewNum = parentNewNum + "." + counter;
        }

        String name = extractName(entry.getColProductSystem());
        int docLevel = currentNewNum.split("\\.").length + 2;
        docLevel = Math.min(Math.max(docLevel, 3), MAX_HEADING_LEVEL);
        addNumberedHeading(doc, name, docLevel, numId);

        String description = entry.getColFeatureDesc();
        if (description != null && !description.isBlank()) {
            processDescriptionWithImages(doc, description);
        }

        List<DataEntry> children = childrenMap.get(entry.getId());
        if (children != null) {
            for (DataEntry child : children) {
                processNodeAndChildren(doc, child, currentNewNum, childrenMap, numId, level3Counter, childCounters);
            }
        }
    }

    private void processDescriptionWithImages(XWPFDocument doc, String description) {
        List<String> parts = new ArrayList<>();
        Matcher matcher = URL_PATTERN.matcher(description);
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
            if (part.isEmpty()) {
                i++;
                continue;
            }

            if (part.startsWith("http://") || part.startsWith("https://")) {
                List<String> consecutiveUrls = new ArrayList<>();
                consecutiveUrls.add(part);
                int j = i + 1;
                while (j < parts.size()) {
                    String nextPart = parts.get(j).trim();
                    if (nextPart.isEmpty()) {
                        j++;
                        continue;
                    }
                    if (nextPart.startsWith("http://") || nextPart.startsWith("https://")) {
                        consecutiveUrls.add(nextPart);
                        j++;
                    } else {
                        break;
                    }
                }

                if (consecutiveUrls.size() >= 2) {
                    List<ImageData> groupImages = new ArrayList<>();
                    List<String> groupUrls = new ArrayList<>();
                    boolean allSuccess = true;
                    boolean isPortraitGroup = false;

                    for (String url : consecutiveUrls) {
                        ImageData imgData = downloadAndProcessImage(url);
                        if (imgData == null) {
                            allSuccess = false;
                            break;
                        }
                        if (imgData.height > imgData.width * PORTRAIT_RATIO) {
                            isPortraitGroup = true;
                        }
                        groupImages.add(imgData);
                        groupUrls.add(url);
                    }

                    if (allSuccess && isPortraitGroup && groupImages.size() >= 2) {
                        insertImageGrid(doc, groupImages, groupUrls);
                    } else {
                        for (String url : consecutiveUrls) {
                            ImageData imgData = downloadAndProcessImage(url);
                            if (imgData != null) {
                                insertSingleImage(doc, url, imgData);
                            } else {
                                insertFallbackText(doc, url);
                            }
                        }
                    }
                } else {
                    String url = consecutiveUrls.get(0);
                    ImageData imgData = downloadAndProcessImage(url);
                    if (imgData != null) {
                        insertSingleImage(doc, url, imgData);
                    } else {
                        insertFallbackText(doc, url);
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

    private ImageData downloadAndProcessImage(String url) {
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
            if (responseCode != 200) {
                return null;
            }

            String contentType = conn.getContentType();
            if (contentType != null && contentType.startsWith("text/")) {
                return null;
            }

            byte[] imageData = conn.getInputStream().readAllBytes();
            if (imageData.length == 0) {
                return null;
            }

            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageData));
            if (img == null) {
                return null;
            }

            int width = img.getWidth();
            int height = img.getHeight();

            String lowerUrl = url.toLowerCase();
            int pictureType;
            if (lowerUrl.contains(".png")) {
                pictureType = XWPFDocument.PICTURE_TYPE_PNG;
            } else if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
                pictureType = XWPFDocument.PICTURE_TYPE_JPEG;
            } else if (lowerUrl.contains(".gif")) {
                pictureType = XWPFDocument.PICTURE_TYPE_GIF;
            } else if (lowerUrl.contains(".bmp")) {
                pictureType = XWPFDocument.PICTURE_TYPE_BMP;
            } else {
                ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
                ImageIO.write(img, "png", pngOut);
                imageData = pngOut.toByteArray();
                pictureType = XWPFDocument.PICTURE_TYPE_PNG;
            }

            return new ImageData(imageData, width, height, pictureType);
        } catch (Exception e) {
            log.warn("Failed to download image: {}", url, e);
            return null;
        }
    }

    private void insertSingleImage(XWPFDocument doc, String url, ImageData imgData) {
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

            XWPFParagraph para = doc.createParagraph();
            para.setAlignment(ParagraphAlignment.CENTER);
            para.setIndentationFirstLine(420);
            XWPFRun run = para.createRun();
            run.addPicture(new ByteArrayInputStream(imgData.data), imgData.pictureType,
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
            insertFallbackText(doc, url);
        }
    }

    private void insertImageGrid(XWPFDocument doc, List<ImageData> images, List<String> urls) {
        int numCols = Math.min(images.size(), 3);
        int numRows = (int) Math.ceil((double) images.size() / numCols);

        XWPFTable table = doc.createTable(numRows, numCols);

        int imgIdx = 0;
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                XWPFTableCell cell = table.getRow(r).getCell(c);
                cell.removeParagraph(0);
                XWPFParagraph cellPara = cell.addParagraph();
                cellPara.setAlignment(ParagraphAlignment.CENTER);

                if (imgIdx < images.size()) {
                    ImageData img = images.get(imgIdx);
                    String imgUrl = urls.get(imgIdx);

                    double targetHeightPx = IMAGE_PORTRAIT_HEIGHT_PX;
                    double aspectRatio = (double) img.width / img.height;
                    double targetWidthPx = targetHeightPx * aspectRatio;

                    int widthEMU = (int) (targetWidthPx * 9525);
                    int heightEMU = (int) (targetHeightPx * 9525);

                    try {
                        XWPFRun run = cellPara.createRun();
                        run.addPicture(new ByteArrayInputStream(img.data), img.pictureType,
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

    private void insertFallbackText(XWPFDocument doc, String url) {
        XWPFParagraph para = doc.createParagraph();
        para.setIndentationFirstLine(420);
        para.setAlignment(ParagraphAlignment.LEFT);
        para.setSpacingBetween(1.5);
        XWPFRun run = para.createRun();
        run.setText("<" + url + ">");
        setFontStyle(run);
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
            String path = new URL(url).getPath();
            String decoded = URLDecoder.decode(path, "UTF-8");
            String filename = decoded.substring(decoded.lastIndexOf('/') + 1);
            int queryIdx = filename.indexOf('?');
            if (queryIdx >= 0) filename = filename.substring(0, queryIdx);
            int dotIdx = filename.lastIndexOf('.');
            if (dotIdx >= 0) filename = filename.substring(0, dotIdx);
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

    private BigInteger initMultilevelNumbering(XWPFDocument doc) {
        String[] lvlTexts = {"%1", "%1.%2", "%1.%2.%3", "%1.%2.%3.%4",
                "%1.%2.%3.%4.%5", "%1.%2.%3.%4.%5.%6",
                "%1.%2.%3.%4.%5.%6.%7", "%1.%2.%3.%4.%5.%6.%7.%8",
                "%1.%2.%3.%4.%5.%6.%7.%8.%9"};

        StringBuilder sb = new StringBuilder();
        sb.append("<w:abstractNum xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" w:multiLevelType=\"hybridMultilevel\">");
        for (int l = 0; l < 9; l++) {
            sb.append("<w:lvl w:ilvl=\"").append(l).append("\">");
            sb.append("<w:start w:val=\"1\"/>");
            sb.append("<w:numFmt w:val=\"decimal\"/>");
            sb.append("<w:lvlText w:val=\"").append(lvlTexts[l]).append("\"/>");
            sb.append("<w:lvlJc w:val=\"left\"/>");
            sb.append("<w:suff w:val=\"Space\"/>");
            sb.append("<w:pPr><w:ind w:left=\"").append(960 * l).append("\" w:hanging=\"480\"/></w:pPr>");
            sb.append("</w:lvl>");
        }
        sb.append("</w:abstractNum>");

        XWPFNumbering numbering = doc.createNumbering();
        CTAbstractNum ctAbstractNum;
        try {
            ctAbstractNum = CTAbstractNum.Factory.parse(sb.toString());
        } catch (Exception e) {
            log.error("Failed to parse numbering XML", e);
            return null;
        }

        XWPFAbstractNum abstractNum = new XWPFAbstractNum(ctAbstractNum, numbering);
        BigInteger abstractNumId = numbering.addAbstractNum(abstractNum);
        BigInteger numId = numbering.addNum(abstractNumId);

        return numId;
    }

    private void addNumberedHeading(XWPFDocument doc, String text, int level, BigInteger numId) {
        XWPFParagraph para = doc.createParagraph();
        para.setStyle("Heading" + level);
        para.setAlignment(ParagraphAlignment.LEFT);
        para.setSpacingBetween(1.5);

        CTPPr pPr = para.getCTP().isSetPPr() ? para.getCTP().getPPr() : para.getCTP().addNewPPr();

        CTDecimalNumber outlineLvl = pPr.addNewOutlineLvl();
        outlineLvl.setVal(BigInteger.valueOf(level - 1));

        CTNumPr numPr = pPr.addNewNumPr();
        numPr.addNewIlvl().setVal(BigInteger.valueOf(level - 1));
        numPr.addNewNumId().setVal(numId);

        XWPFRun run = para.createRun();
        run.setText(text);
        setFontStyle(run);
        if (level >= 1 && level <= 4) {
            run.setBold(true);
        }
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
        StringBuilder sb = new StringBuilder();
        sb.append("<w:styles xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">");

        sb.append("<w:docDefaults>");
        sb.append("<w:rPrDefault><w:rPr>");
        sb.append("<w:rFonts w:ascii=\"").append(fontTheme).append("\" w:eastAsia=\"").append(fontTheme)
                .append("\" w:hAnsi=\"").append(fontTheme).append("\" w:cs=\"").append(fontTheme).append("\"/>");
        sb.append("<w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/>");
        sb.append("<w:lang w:val=\"zh-CN\" w:eastAsia=\"zh-CN\" w:bidi=\"ar-SA\"/>");
        sb.append("</w:rPr></w:rPrDefault>");
        sb.append("<w:pPrDefault><w:pPr>");
        sb.append("<w:spacing w:after=\"200\" w:line=\"276\" w:lineRule=\"auto\"/>");
        sb.append("</w:pPr></w:pPrDefault>");
        sb.append("</w:docDefaults>");

        sb.append("<w:latentStyles w:defLockedState=\"0\" w:defUIPriority=\"99\" w:defSemiHidden=\"1\" "
                + "w:defUnhideWhenUsed=\"1\" w:defQFormat=\"0\" w:count=\"276\">");
        sb.append("<w:lsdException w:name=\"Normal\" w:semiHidden=\"0\" w:uiPriority=\"0\" w:unhideWhenUsed=\"0\" w:qFormat=\"1\"/>");
        for (int i = 1; i <= 9; i++) {
            sb.append("<w:lsdException w:name=\"heading ").append(i).append("\" w:uiPriority=\"9\" w:qFormat=\"1\"/>");
        }
        sb.append("<w:lsdException w:name=\"toc 1\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 2\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 3\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 4\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 5\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 6\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 7\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 8\" w:uiPriority=\"39\"/>");
        sb.append("<w:lsdException w:name=\"toc 9\" w:uiPriority=\"39\"/>");
        sb.append("</w:latentStyles>");

        sb.append("<w:style w:type=\"paragraph\" w:default=\"1\" w:styleId=\"Normal\">");
        sb.append("<w:name w:val=\"Normal\"/>");
        sb.append("<w:qFormat/>");
        sb.append("<w:pPr><w:spacing w:line=\"360\" w:lineRule=\"auto\"/></w:pPr>");
        sb.append("</w:style>");

        boolean[] headingBold = {true, true, true, true, false, false, false, false, false};
        boolean[] headingItalic = {false, false, false, true, false, true, true, false, true};
        Integer[] headingSizes = {28, 26, null, null, null, null, null, 20, 20};

        for (int level = 1; level <= 9; level++) {
            sb.append("<w:style w:type=\"paragraph\" w:styleId=\"Heading").append(level).append("\">");
            sb.append("<w:name w:val=\"heading ").append(level).append("\"/>");
            sb.append("<w:basedOn w:val=\"Normal\"/>");
            sb.append("<w:next w:val=\"Normal\"/>");
            sb.append("<w:uiPriority w:val=\"9\"/>");
            sb.append("<w:qFormat/>");
            sb.append("<w:pPr>");
            sb.append("<w:keepNext/><w:keepLines/>");
            sb.append("<w:spacing w:before=\"").append(level == 1 ? "480" : "200").append("\" w:after=\"0\"/>");
            sb.append("<w:outlineLvl w:val=\"").append(level - 1).append("\"/>");
            sb.append("</w:pPr>");
            sb.append("<w:rPr>");
            sb.append("<w:rFonts w:ascii=\"").append(fontTheme).append("\" w:eastAsia=\"").append(fontTheme)
                    .append("\" w:hAnsi=\"").append(fontTheme).append("\" w:cs=\"").append(fontTheme).append("\"/>");
            if (headingBold[level - 1]) sb.append("<w:b/><w:bCs/>");
            if (headingItalic[level - 1]) sb.append("<w:i/><w:iCs/>");
            if (headingSizes[level - 1] != null) {
                sb.append("<w:sz w:val=\"").append(headingSizes[level - 1]).append("\"/>");
                sb.append("<w:szCs w:val=\"").append(headingSizes[level - 1]).append("\"/>");
            }
            sb.append("</w:rPr>");
            sb.append("</w:style>");
        }

        sb.append("</w:styles>");

        try {
            CTStyles ctStyles = CTStyles.Factory.parse(sb.toString());
            styles.setStyles(ctStyles);
        } catch (Exception e) {
            log.error("Failed to create built-in heading styles", e);
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
