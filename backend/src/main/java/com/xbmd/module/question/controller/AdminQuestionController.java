package com.xbmd.module.question.controller;

import com.xbmd.common.PageResult;
import com.xbmd.common.Result;
import com.xbmd.module.question.dto.QuestionQuery;
import com.xbmd.module.question.dto.QuestionRequest;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.service.QuestionService;
import com.xbmd.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/admin/questions")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminQuestionController {

    private final QuestionService questionService;

    /** 分页查询（管理端可查下架 status=0 / 全量 status=null） */
    @GetMapping
    public Result<PageResult<QuestionVO>> page(QuestionQuery query) {
        return Result.success(questionService.pageQuery(query));
    }

    @PostMapping
    public Result<QuestionVO> create(@Valid @RequestBody QuestionRequest request) {
        return Result.success(questionService.create(request));
    }

    @PutMapping("/{id}")
    public Result<QuestionVO> update(@PathVariable Long id, @Valid @RequestBody QuestionRequest request) {
        return Result.success(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return Result.success();
    }

    /** 上下架 */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        questionService.updateStatus(id, status);
        return Result.success();
    }
}
