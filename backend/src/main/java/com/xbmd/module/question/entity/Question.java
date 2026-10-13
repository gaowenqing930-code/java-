package com.xbmd.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目表
 */
@Data
@TableName("question")
public class Question {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题型：SINGLE/MULTIPLE/JUDGE/SHORT */
    private String type;

    /** 难度 1-5 */
    private Integer difficulty;

    /** 题干 */
    private String stem;

    /** 选项 JSON：[{"key":"A","text":"..."}] */
    private String optionsJson;

    /** 参考答案 */
    private String answer;

    /** 评分点 JSON：[{"point":"...","score":2}]（简答题） */
    private String scorePointsJson;

    /** 解析 */
    private String analysis;

    /** 来源：MANUAL/AI */
    private String source;

    /** 题目指纹（SHA-256） */
    private String fingerprint;

    /** 状态：1上架 0下架/删除 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 非表字段：关联的知识点 ID */
    @TableField(exist = false)
    private java.util.List<Long> categoryIds;
}
