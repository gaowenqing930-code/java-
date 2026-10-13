package com.xbmd.module.practice.controller;

import com.xbmd.common.Result;
import com.xbmd.module.practice.dto.AnswerSubmitRequest;
import com.xbmd.module.practice.dto.AnswerSubmitResult;
import com.xbmd.module.practice.dto.CreatePracticeRequest;
import com.xbmd.module.practice.dto.PracticeDetailVO;
import com.xbmd.module.practice.dto.PracticeHistoryVO;
import com.xbmd.module.practice.dto.SubmitAllRequest;
import com.xbmd.module.practice.service.PracticeService;
import com.xbmd.security.UserContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在线练习接口（登录用户，仅本人数据）
 */
@RestController
@RequestMapping("/api/practices")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    /** 创建练习（题库/AI/自适应/错题重练四种模式） */
    @PostMapping
    public Result<PracticeDetailVO> create(@Valid @RequestBody CreatePracticeRequest request) {
        return Result.success(practiceService.create(request, UserContext.getUserId()));
    }

    /** 提交单题答案（幂等：同练习同题重复提交返回已有结果） */
    @PostMapping("/{id}/answers")
    public Result<AnswerSubmitResult> submit(@PathVariable Long id,
                                             @Valid @RequestBody AnswerSubmitRequest request) {
        return Result.success(practiceService.submitAnswer(id, request, UserContext.getUserId()));
    }

    /** 交卷：批量提交并结算成绩 */
    @PostMapping("/{id}/submit")
    public Result<PracticeDetailVO> submitAll(@PathVariable Long id,
                                              @Valid @RequestBody SubmitAllRequest request) {
        return Result.success(practiceService.submitAll(id, request.getAnswers(), UserContext.getUserId()));
    }

    /** 练习详情（含逐题结果） */
    @GetMapping("/{id}")
    public Result<PracticeDetailVO> detail(@PathVariable Long id) {
        return Result.success(practiceService.detail(id, UserContext.getUserId()));
    }

    /** 历史答题记录（本人已完成练习，按时间倒序） */
    @GetMapping("/history")
    public Result<List<PracticeHistoryVO>> history() {
        return Result.success(practiceService.history(UserContext.getUserId()));
    }
}
