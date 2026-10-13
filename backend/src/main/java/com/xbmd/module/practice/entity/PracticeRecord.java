package com.xbmd.module.practice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 练习记录表
 */
@Data
@TableName("practice_record")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 练习方式：PAPER_BANK/AI/ADAPTIVE/WRONG */
    private String mode;

    /** 组题条件快照（JSON） */
    private String conditionJson;

    /** 总题数 */
    private Integer totalCount;

    /** 答对题数 */
    private Integer correctCount;

    /** 总得分（每题满分10，主观题按评测分） */
    private Integer score;

    /** 状态：0进行中 1已完成 */
    private Integer status;

    private LocalDateTime createTime;

    /** 完成时间 */
    private LocalDateTime finishTime;
}
