package com.superpower.modules.document.service;

import com.superpower.modules.data.entity.DataEntry;
import com.superpower.modules.data.repository.DataEntryRepository;
import com.superpower.modules.document.repository.DocGenRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DataEntryRepository entryRepository;

    @Mock
    private DocGenRecordRepository docGenRecordRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void shouldGenerateWordWithBuiltinHeadingStylesForFeatureDocument() throws Exception {
        DataEntry entry = new DataEntry();
        entry.setId(1L);
        entry.setLevel(3);
        entry.setSortOrder(1);
        entry.setColBizCategory("1. 数智底座-数据");
        entry.setColBizDomain("1.1 大数据平台");
        entry.setColProductSystem("1.1.1 数据资源管理平台");
        entry.setColFeatureDesc("这是功能说明");

        when(entryRepository.findAllById(List.of(1L))).thenReturn(List.of(entry));

        byte[] bytes = documentService.generateDocument("feature", "word", List.of(1L));

        String stylesXml = readZipEntry(bytes, "word/styles.xml");
        String documentXml = readZipEntry(bytes, "word/document.xml");

        assertNotNull(stylesXml, "生成的 Word 文档必须包含 word/styles.xml，才能让 Word/WPS 识别原生标题样式");
        assertTrue(stylesXml.contains("styleId=\"Heading1\"") || stylesXml.contains("styleId=\"heading 1\""),
                "styles.xml 应定义 Heading1 样式");
        assertTrue(documentXml.contains("<w:pStyle w:val=\"Heading1\"/>") || documentXml.contains("<w:pStyle w:val=\"heading 1\"/>"),
                "文档中的一级标题应引用内建标题样式");
    }

    private String readZipEntry(byte[] bytes, String entryName) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    zis.transferTo(out);
                    return out.toString(StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
