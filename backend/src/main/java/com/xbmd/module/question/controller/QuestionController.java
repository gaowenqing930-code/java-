package com.xbmd.module.question.controller;

import com.xbmd.common.PageResult;
import com.xbmd.common.Result;
import com.xbmd.module.question.dto.QuestionQuery;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目查询接口（登录用户）
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    /** 五维组合筛选：领域+方向+知识点+题型+难度+关键词 */
    @GetMapping
    public Result<PageResult<QuestionVO>> list(QuestionQuery query) {
        return Result.success(questionService.pageQuery(query));
    }

    /** 题目详情 */
    @GetMapping("/{id}")
    public Result<QuestionVO> detail(@PathVariable Long id) {
        return Result.success(questionService.getDetail(id));
    }

    /** 查询参数便捷绑定（与 QuestionQuery 字段一致） */
    @GetMapping("/page")
    public Result<PageResult<QuestionVO>> page(@RequestParam(required = false) Long domainId,
                                               @RequestParam(required = false) Long directionId,
                                               @RequestParam(required = false) Long pointId,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Integer difficulty,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        QuestionQuery query = new QuestionQuery();
        query.setDomainId(domainId);
        query.setDirectionId(directionId);
        query.setPointId(pointId);
        query.setType(type);
        query.setDifficulty(difficulty);
        query.setKeyword(keyword);
        query.setStatus(1);
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        return Result.success(questionService.pageQuery(query));
    }
}
