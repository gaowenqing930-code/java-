package com.xbmd.module.interview.dto;

import lombok.Data;

/**
 * 简历文件文本提取结果
 */
@Data
public class ResumeExtractVO {

    /** 原始文件名（含扩展名） */
    private String fileName;

    /** 提取出的简历文本（供用户确认/修改后走 AI 解析） */
    private String text;
}
