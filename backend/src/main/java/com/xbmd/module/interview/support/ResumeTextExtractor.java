package com.xbmd.module.interview.support;

import com.xbmd.common.BusinessException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 简历文件文本提取：支持 .docx / .doc / .pdf。
 * 提取结果用于简历解析（AI 抽取技术栈与项目经历），不保存原始文件。
 */
@Component
public class ResumeTextExtractor {

    /** 提取文本的最大字符数，超长截断，防止把超长简历灌给模型 */
    private static final int MAX_CHARS = 8000;

    public String extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "请先选择要上传的简历文件");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            throw new BusinessException(400, "不支持的文件格式，请上传 .docx / .doc / .pdf 格式的简历");
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
        String text;
        try {
            switch (ext) {
                case "docx" -> text = extractDocx(file.getInputStream());
                case "doc" -> text = extractDoc(file.getInputStream());
                case "pdf" -> text = extractPdf(file.getInputStream());
                default -> throw new BusinessException(400, "不支持的文件格式，请上传 .docx / .doc / .pdf 格式的简历");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(500, "简历文件解析失败，请确认文件未损坏");
        }
        if (text == null || text.isBlank()) {
            throw new BusinessException(400, "未能从文件中提取到文本内容，请检查文件是否为文本型简历（扫描件/图片 PDF 暂不支持）");
        }
        String trimmed = text.trim();
        if (trimmed.length() > MAX_CHARS) {
            trimmed = trimmed.substring(0, MAX_CHARS);
        }
        return trimmed;
    }

    /** .docx：段落 + 表格文本 */
    private String extractDocx(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                appendLine(sb, p.getText());
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    StringBuilder rowSb = new StringBuilder();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        if (cell.getText() != null && !cell.getText().isBlank()) {
                            if (!rowSb.isEmpty()) {
                                rowSb.append(" | ");
                            }
                            rowSb.append(cell.getText().trim());
                        }
                    }
                    appendLine(sb, rowSb.toString());
                }
            }
        }
        return sb.toString();
    }

    /** .doc（旧版二进制格式，POI HWPF） */
    private String extractDoc(InputStream in) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(in);
             WordExtractor extractor = new WordExtractor(doc)) {
            return String.join("\n", extractor.getParagraphText());
        }
    }

    /** .pdf：文本层提取（扫描件/图片型 PDF 无文本层，将返回空） */
    private String extractPdf(InputStream in) throws IOException {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private void appendLine(StringBuilder sb, String line) {
        if (line != null && !line.isBlank()) {
            sb.append(line.trim()).append('\n');
        }
    }
}
