package com.xbmd.module.knowledge.controller;

import com.xbmd.common.Result;
import com.xbmd.module.knowledge.dto.CategoryRequest;
import com.xbmd.module.knowledge.entity.KnowledgeCategory;
import com.xbmd.module.knowledge.service.KnowledgeCategoryService;
import com.xbmd.security.RequireRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识分类管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequireRole("ADMIN")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final KnowledgeCategoryService categoryService;

    @PostMapping
    public Result<KnowledgeCategory> create(@Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.create(request));
    }

    @PutMapping("/{id}")
    public Result<KnowledgeCategory> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return Result.success(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
