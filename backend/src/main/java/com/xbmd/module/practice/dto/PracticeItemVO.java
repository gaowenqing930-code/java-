package com.xbmd.module.practice.dto;

import com.xbmd.module.evaluation.dto.EvaluationVO;
import com.xbmd.module.question.dto.QuestionVO;
import lombok.Data;

/**
 * 练习详情中的单题（题目 + 作答 + 判分 + 评测）
 */
@Data
public class PracticeItemVO {

    /** 题目（含答案解析） */
    private QuestionVO question;

    /** 答题记录 ID（评测重试用） */
    private Long answerRecordId;

    /** 用户答案（未作答为 null） */
    private String userAnswer;

    /** 是否答对：true/false，主观题待评测为 null */
    private Boolean correct;

    /** 本题得分（客观题10/0，主观题按评测；未作答为 null） */
    private Integer score;

    /** 主观题评测结果（客观题为 null） */
    private EvaluationVO evaluation;
}
