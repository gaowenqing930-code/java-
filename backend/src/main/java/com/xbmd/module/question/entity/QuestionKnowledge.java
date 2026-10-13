package com.xbmd.module.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 题目-知识点关联表
 */
@Data
@TableName("question_knowledge")
public class QuestionKnowledge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    /** 知识点分类 ID（level=3） */
    private Long categoryId;
}
