package com.xbmd.module.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错题表（Task 7 错题管理模块，Task 5 错题重练模式依赖）
 */
@Data
@TableName("wrong_question")
public class WrongQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 题目 ID */
    private Long questionId;

    /** 最近一次答错的答题记录 ID */
    private Long lastAnswerId;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 状态：1未掌握 0已掌握/移除 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
