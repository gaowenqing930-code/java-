package com.xbmd.module.exam.controller;

import com.xbmd.common.Result;
import com.xbmd.module.exam.dto.ExamPaperVO;
import com.xbmd.module.exam.service.ExamPaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 真题卷子接口：列表 / 详情（登录可访问）
 */
@RestController
@RequestMapping("/api/exam-papers")
@RequiredArgsConstructor
public class ExamPaperController {

    private final ExamPaperService examPaperService;

    /** 卷子列表（可按类型过滤 WRITTEN/INTERVIEW） */
    @GetMapping
    public Result<List<ExamPaperVO>> list(@RequestParam(required = false) String type) {
        return Result.success(examPaperService.listPapers(type));
    }

    /** 卷子详情（含固定题目） */
    @GetMapping("/{id}")
    public Result<ExamPaperVO> detail(@PathVariable Long id) {
        return Result.success(examPaperService.getPaper(id));
    }
}
