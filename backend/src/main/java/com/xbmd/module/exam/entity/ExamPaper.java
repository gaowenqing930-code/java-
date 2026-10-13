package com.xbmd.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 真题卷子（预置固定题目，如企业笔试/面试真题）
 */
@Data
@TableName("exam_paper")
public class ExamPaper {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 企业名称 */
    private String company;

    /** 年份 */
    private Integer year;

    /** 场次季节：秋招/春招/社招 */
    private String season;

    /** 具体场次：如 9月17号/一面/第一批笔试 */
    private String round;

    /** 面向岗位：如 Java 工程师 */
    private String position;

    /** 类型：笔试/面试 */
    private String type;

    /** 热度（浏览量） */
    private Integer heat;

    /** 是否热门企业：1是 0否 */
    private Integer hot;

    /** 收录题数（与关联表保持一致） */
    private Integer questionCount;

    /** 状态：1上架 0下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
