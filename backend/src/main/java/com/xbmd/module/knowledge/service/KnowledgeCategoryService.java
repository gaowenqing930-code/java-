package com.xbmd.module.knowledge.service;

import com.xbmd.module.knowledge.dto.CategoryNode;
import com.xbmd.module.knowledge.dto.CategoryRequest;
import com.xbmd.module.knowledge.entity.KnowledgeCategory;

import java.util.List;

/**
 * 知识分类服务
 */
public interface KnowledgeCategoryService {

    /** 查询完整分类树 */
    List<CategoryNode> getTree();

    /** 新增分类（校验父节点与层级合法性） */
    KnowledgeCategory create(CategoryRequest request);

    /** 修改分类 */
    KnowledgeCategory update(Long id, CategoryRequest request);

    /** 删除分类：有子节点或被题目引用时禁止删除 */
    void delete(Long id);

    /** 获取指定节点及其全部子孙节点中 level=3 的知识点 ID 集合 */
    List<Long> collectKnowledgePointIds(Long categoryId);

    /** 获取全量分类列表（Redis 缓存，供抽题/出题/统计热路径复用，避免每次全表扫描） */
    List<KnowledgeCategory> getAllCached();
}
