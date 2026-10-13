package com.xbmd.module.interview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 面试总结（模型输出）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSummary {

    private String summary;

    private String suggestion;
}
