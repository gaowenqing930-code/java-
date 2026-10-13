package com.xbmd.module.interview.dto;

import com.xbmd.module.question.dto.ScorePointDTO;
import lombok.Data;

import java.util.List;

/**
 * 面试题视图：未答时只返回题干，答完返回评分与参考回答
 */
@Data
public class InterviewQuestionVO {

    private Integer no;

    private String question;

    /** PENDING / SCORED */
    private String status;

    private String userAnswer;

    private Integer aiScore;

    private String aiComment;

    private List<ScorePointDTO> scorePoints;

    private String referenceAnswer;

    /** AI 建议是否可结束面试（仅当前题返回时有效） */
    private Boolean suggestFinish;

    /** 建议结束的原因 */
    private String finishReason;
}
