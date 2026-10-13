package com.xbmd.module.practice.dto;

import com.xbmd.module.evaluation.dto.EvaluationVO;
import lombok.Data;

/**
 * 单题提交答案结果
 */
@Data
public class AnswerSubmitResult {

    private Long answerId;

    private Long questionId;

    /** 是否答对：客观题 true/false；主观题评测后回填，失败为 null（待评测） */
    private Boolean correct;

    /** 本题得分 */
    private Integer score;

    /** 主观题评测结果 */
    private EvaluationVO evaluation;

    /** 提示信息（如降级说明） */
    private String message;
}
