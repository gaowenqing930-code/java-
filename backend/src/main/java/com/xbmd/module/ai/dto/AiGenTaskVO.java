package com.xbmd.module.ai.dto;

import com.xbmd.module.question.dto.QuestionVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 出题异步任务 VO：提交任务与查询状态统一返回结构。
 */
@Data
public class AiGenTaskVO {

    /** 任务 id */
    private Long taskId;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    /** 进度 0~100 */
    private Integer progress;

    /** 已生成题数 */
    private Integer done;

    /** 目标题数 */
    private Integer total;

    /** SUCCESS 时的题目列表 */
    private List<QuestionVO> questions;

    /** 提示 / 错误信息 */
    private String message;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
