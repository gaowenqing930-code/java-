package com.xbmd.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 真题套卷题目关联表
 */
@Data
@TableName("exam_paper_question")
public class ExamPaperQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 套卷 ID */
    private Long paperId;

    /** 题目 ID */
    private Long questionId;

    /** 题号顺序，从 1 开始 */
    private Integer sort;
}
