package com.xbmd.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 出题异步任务：提交后立即返回 taskId，后台线程池生成并推进度，SSE/轮询取结果。
 */
@Data
@TableName("ai_gen_task")
public class AiGenTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起用户 */
    private Long userId;

    /** PENDING / RUNNING / SUCCESS / FAILED */
    private String status;

    /** 进度 0~100 */
    private Integer progress;

    /** 出题参数快照（AiGenerateRequest JSON） */
    private String requestJson;

    /** 成功后的题目列表（QuestionVO JSON） */
    private String resultJson;

    /** 失败原因 */
    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
