package com.xbmd.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 面试题目与评分
 */
@Data
@TableName("interview_question")
public class InterviewQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    /** 题号 1-10 */
    private Integer questionNo;

    private String question;

    /** 评分点 JSON [{point,score}] */
    private String scorePointsJson;

    private String referenceAnswer;

    private String userAnswer;

    /** AI 评分 0-10 */
    private Integer aiScore;

    private String aiComment;

    /** PENDING / SCORED */
    private String status;
}
