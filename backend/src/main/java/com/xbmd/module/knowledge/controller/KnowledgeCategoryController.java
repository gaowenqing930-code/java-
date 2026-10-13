package com.xbmd.module.knowledge.controller;

import com.xbmd.common.Result;
import com.xbmd.module.knowledge.dto.CategoryNode;
import com.xbmd.module.knowledge.service.KnowledgeCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识分类查询接口（登录用户）
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class KnowledgeCategoryController {

    private final KnowledgeCategoryService categoryService;

    /** 分类树查询 */
    @GetMapping("/tree")
    public Result<List<CategoryNode>> tree() {
        return Result.success(categoryService.getTree());
    }
}
