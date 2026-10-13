package com.xbmd.module.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 答题记录表
 */
@Data
@TableName("answer_record")
public class AnswerRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 练习 ID */
    private Long practiceId;

    /** 用户 ID */
    private Long userId;

    /** 题目 ID */
    private Long questionId;

    /** 用户答案（选项 key 或文本） */
    private String userAnswer;

    /** 是否答对：1对 0错 NULL待评测 */
    private Integer isCorrect;

    /** 本题得分（客观题10/0，主观题按评测） */
    private Integer score;

    private LocalDateTime createTime;
}
