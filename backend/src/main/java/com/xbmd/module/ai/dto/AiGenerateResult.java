package com.xbmd.module.ai.dto;

import com.xbmd.module.question.dto.QuestionVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 出题结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateResult {

    /** 生成的题目（来源 AI 或兜底题库 MANUAL） */
    private List<QuestionVO> questions;

    /** 是否走题库兜底 */
    private boolean fromFallback;

    /** 提示信息 */
    private String message;
}
