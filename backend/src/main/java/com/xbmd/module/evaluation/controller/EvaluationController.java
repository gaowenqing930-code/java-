package com.xbmd.module.evaluation.controller;

import com.xbmd.common.Result;
import com.xbmd.module.evaluation.dto.EvaluationVO;
import com.xbmd.module.evaluation.service.AiEvaluationService;
import com.xbmd.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 评测接口（主观题重试；评测随答题自动触发）
 */
@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final AiEvaluationService aiEvaluationService;

    /** 重试评测（评测失败/待评测的作答） */
    @PostMapping("/{answerId}/retry")
    public Result<EvaluationVO> retry(@PathVariable Long answerId) {
        return Result.success(aiEvaluationService.retry(answerId, UserContext.getUserId()));
    }
}
