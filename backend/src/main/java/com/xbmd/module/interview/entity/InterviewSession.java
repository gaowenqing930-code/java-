package com.xbmd.module.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 面试会话表
 */
@Data
@TableName("interview_session")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 简历文本（原始） */
    private String resumeText;

    /** 技术栈（逗号分隔） */
    private String techStack;

    /** 项目列表 JSON */
    private String projects;

    /** CREATED / IN_PROGRESS / FINISHED */
    private String status;

    /** 总分（每题满分 10，共 100） */
    private Integer totalScore;

    /** 总体评价 */
    private String summary;

    /** 改进建议 */
    private String suggestion;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
