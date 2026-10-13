package com.xbmd.module.ai.service;

import com.xbmd.module.ai.dto.AiGenerateRequest;
import com.xbmd.module.ai.dto.AiGenerateResult;
import com.xbmd.module.ai.dto.AiGenTaskVO;

/**
 * AI 智能出题服务
 */
public interface AiQuestionService {

    /**
     * 按条件生成题目：
     * 结构化 Prompt → 模型调用 → JSON 解析 → 字段校验 → 指纹去重 → 入库；
     * 失败重试（≤3 次）后仍失败则从题库兜底抽题。
     */
    AiGenerateResult generate(AiGenerateRequest request, Long userId);

    /**
     * 提交异步出题任务：限流 → 建任务(PENDING) → 触发 @Async 后台生成 → 立即返回 taskId。
     * 前端随后通过 SSE/轮询获取进度与结果。
     */
    AiGenTaskVO submitGenerate(AiGenerateRequest request, Long userId);

    /**
     * 查询出题任务状态（供轮询兜底与结果回取），校验任务归属用户。
     */
    AiGenTaskVO getTaskStatus(Long taskId, Long userId);
}
