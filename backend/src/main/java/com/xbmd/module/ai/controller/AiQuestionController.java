package com.xbmd.module.ai.controller;

import com.xbmd.common.Result;
import com.xbmd.module.ai.dto.AiGenerateRequest;
import com.xbmd.module.ai.dto.AiGenerateResult;
import com.xbmd.module.ai.dto.AiGenTaskVO;
import com.xbmd.module.ai.service.AiQuestionService;
import com.xbmd.module.ai.util.SsePusher;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 智能出题接口（登录用户，按用户限流）
 * <p>出题分两段：POST /generate 提交任务立即返回 taskId（后台异步生成），
 * 前端通过 GET /task/{id}/events（SSE）订阅进度/结果，或用 GET /task/{id} 轮询兜底。</p>
 */
@RestController
@RequestMapping("/api/ai/questions")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;
    private final SsePusher ssePusher;

    @PostMapping("/generate")
    public Result<AiGenerateResult> generate(@Valid @RequestBody AiGenerateRequest request) {
        return Result.success(aiQuestionService.generate(request, UserContext.getUserId()));
    }

    /** 提交异步出题任务：立即返回 taskId（不再同步阻塞等待） */
    @PostMapping("/generate/task")
    public Result<AiGenTaskVO> submitGenerate(@Valid @RequestBody AiGenerateRequest request) {
        return Result.success(aiQuestionService.submitGenerate(request, UserContext.getUserId()));
    }

    /** SSE 订阅：实时推送 progress / done / error 事件 */
    @GetMapping("/task/{taskId}/events")
    public SseEmitter events(@PathVariable Long taskId) {
        return ssePusher.subscribe(taskId);
    }

    /** 查询任务状态（SSE 断线时的轮询兜底） */
    @GetMapping("/task/{taskId}")
    public Result<AiGenTaskVO> taskStatus(@PathVariable Long taskId) {
        return Result.success(aiQuestionService.getTaskStatus(taskId, UserContext.getUserId()));
    }
}
