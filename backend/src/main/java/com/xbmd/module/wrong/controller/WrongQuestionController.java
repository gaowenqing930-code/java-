package com.xbmd.module.wrong.controller;

import com.xbmd.common.PageResult;
import com.xbmd.common.Result;
import com.xbmd.module.practice.dto.CreatePracticeRequest;
import com.xbmd.module.practice.dto.PracticeDetailVO;
import com.xbmd.module.practice.service.PracticeService;
import com.xbmd.module.wrong.dto.WrongQuestionVO;
import com.xbmd.module.wrong.service.WrongQuestionService;
import com.xbmd.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 错题管理接口（登录用户，仅本人）
 */
@RestController
@RequestMapping("/api/wrong-questions")
@RequiredArgsConstructor
public class WrongQuestionController {

    private final WrongQuestionService wrongQuestionService;
    private final PracticeService practiceService;

    /** 错题分页列表（按题型/关键词筛选） */
    @GetMapping
    public Result<PageResult<WrongQuestionVO>> list(@RequestParam(required = false) String type,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(wrongQuestionService.pageQuery(
                UserContext.getUserId(), type, keyword, pageNum, pageSize));
    }

    /** 移除错题（标记已掌握） */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        wrongQuestionService.remove(id, UserContext.getUserId());
        return Result.success(null);
    }

    /** 一键重练：创建错题重练练习 */
    @PostMapping("/repractice")
    public Result<PracticeDetailVO> repractice(@RequestParam(defaultValue = "10") int count) {
        CreatePracticeRequest request = new CreatePracticeRequest();
        request.setMode("WRONG");
        request.setCount(count);
        return Result.success(practiceService.create(request, UserContext.getUserId()));
    }
}
