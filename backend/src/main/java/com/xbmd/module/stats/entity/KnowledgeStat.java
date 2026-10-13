package com.xbmd.module.stats.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户知识点统计表（答题后按题目挂载的知识点更新）
 */
@Data
@TableName("knowledge_stat")
public class KnowledgeStat {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 知识点 ID（level=3） */
    private Long categoryId;

    /** 累计答题数 */
    private Integer totalCount;

    /** 累计答对数 */
    private Integer correctCount;

    /** 掌握度 0-100 */
    private BigDecimal mastery;

    private LocalDateTime updateTime;
}
