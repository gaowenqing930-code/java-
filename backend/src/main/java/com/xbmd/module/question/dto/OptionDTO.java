package com.xbmd.module.question.dto;

import lombok.Data;

/**
 * 选择题选项
 */
@Data
public class OptionDTO {

    /** 选项标识：A/B/C/D */
    private String key;

    /** 选项内容 */
    private String text;
}
