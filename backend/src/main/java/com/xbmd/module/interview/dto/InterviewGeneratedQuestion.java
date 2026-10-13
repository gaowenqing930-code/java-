package com.xbmd.module.interview.dto;

import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.Data;

import java.util.List;

/**
 * AI 生成的面试题（模型输出）
 */
@Data
public class InterviewGeneratedQuestion {

    private String question;

    private List<ScorePointDTO> scorePoints;

    private String referenceAnswer;

    /** AI 判断：当前已充分测评简历技术栈，建议结束面试 */
    private Boolean suggestFinish;

    /** 建议结束的原因（suggestFinish=true 时给出） */
    private String finishReason;
}
