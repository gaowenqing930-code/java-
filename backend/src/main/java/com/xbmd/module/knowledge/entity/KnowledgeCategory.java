package com.xbmd.module.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识分类表（自关联树：领域(1) → 方向(2) → 知识点(3)）
 */
@Data
@TableName("knowledge_category")
public class KnowledgeCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类 ID，0 表示根 */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 层级：1领域 2方向 3知识点 */
    private Integer level;

    /** 排序值 */
    private Integer sort;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
