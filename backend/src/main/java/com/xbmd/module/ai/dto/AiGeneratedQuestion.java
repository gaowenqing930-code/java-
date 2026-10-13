package com.xbmd.module.ai.dto;

import com.xbmd.module.question.dto.OptionDTO;
import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.Data;

import java.util.List;

/**
 * 大模型返回的题目（解析目标 DTO）
 */
@Data
public class AiGeneratedQuestion {

    /** 题干 */
    private String stem;

    /** 选择题选项 */
    private List<OptionDTO> options;

    /** 参考答案 */
    private String answer;

    /** 简答题评分点 */
    private List<ScorePointDTO> scorePoints;

    /** 解析 */
    private String analysis;
}
