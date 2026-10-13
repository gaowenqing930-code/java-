package com.xbmd.module.wrong.dto;

import com.xbmd.module.question.dto.QuestionVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题返回体：错题信息 + 题目详情
 */
@Data
public class WrongQuestionVO {

    private Long id;

    /** 题目详情 */
    private QuestionVO question;

    /** 最近一次答错记录 ID */
    private Long lastAnswerId;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 状态：1未掌握 0已移除 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
