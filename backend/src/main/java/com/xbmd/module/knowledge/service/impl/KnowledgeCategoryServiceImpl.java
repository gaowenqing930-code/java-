package com.xbmd.module.knowledge.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.knowledge.dto.CategoryNode;
import com.xbmd.module.knowledge.dto.CategoryRequest;
import com.xbmd.module.knowledge.entity.KnowledgeCategory;
import com.xbmd.module.knowledge.mapper.KnowledgeCategoryMapper;
import com.xbmd.module.knowledge.service.KnowledgeCategoryService;
import com.xbmd.module.question.mapper.QuestionKnowledgeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识分类服务实现
 */
@Service
@RequiredArgsConstructor
public class KnowledgeCategoryServiceImpl implements KnowledgeCategoryService {

    private final KnowledgeCategoryMapper categoryMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 全量分类列表缓存 key（供 collectKnowledgePointIds 等热路径复用，避免每次全表扫描） */
    private static final String ALL_KEY = "knowledge:all";

    @Cacheable(value = "knowledgeTree")
    @Override
    public List<CategoryNode> getTree() {
        return buildTree(getAllCached());
    }

    @Override
    public KnowledgeCategory create(CategoryRequest request) {
        validateParentAndLevel(request.getParentId(), request.getLevel());
        KnowledgeCategory category = new KnowledgeCategory();
        category.setParentId(request.getParentId());
        category.setName(request.getName().trim());
        category.setLevel(request.getLevel());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        categoryMapper.insert(category);
        evictAllCache();
        return category;
    }

    @CacheEvict(value = "knowledgeTree", allEntries = true)
    @Override
    public KnowledgeCategory update(Long id, CategoryRequest request) {
        KnowledgeCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        // 修改层级时同步校验父节点
        if (!exist.getParentId().equals(request.getParentId()) || !exist.getLevel().equals(request.getLevel())) {
            validateParentAndLevel(request.getParentId(), request.getLevel());
            // 禁止把节点挂到自己的子孙下
            List<Long> descendants = collectDescendantIds(id);
            if (descendants.contains(request.getParentId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "父节点不能是自己的子孙节点");
            }
        }
        exist.setParentId(request.getParentId());
        exist.setName(request.getName().trim());
        exist.setLevel(request.getLevel());
        exist.setSort(request.getSort() == null ? 0 : request.getSort());
        categoryMapper.updateById(exist);
        evictAllCache();
        return exist;
    }

    @Override
    public void delete(Long id) {
        KnowledgeCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "分类不存在");
        }
        // 有子节点禁止删除
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeCategory>().eq(KnowledgeCategory::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该分类下存在子分类，禁止删除");
        }
        // 被题目引用禁止删除
        Long refCount = questionKnowledgeMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.xbmd.module.question.entity.QuestionKnowledge>()
                        .eq("category_id", id));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该分类已被题目引用，禁止删除");
        }
        categoryMapper.deleteById(id);
        evictAllCache();
    }

    @Override
    public List<Long> collectKnowledgePointIds(Long categoryId) {
        List<KnowledgeCategory> all = getAllCached();
        return collectPointIds(all, categoryId);
    }

    /** 获取全量分类列表（Redis 缓存，TTL 1h；写操作时清缓存） */
    @Override
    public List<KnowledgeCategory> getAllCached() {
        String json = redisTemplate.opsForValue().get(ALL_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(json, new TypeReference<List<KnowledgeCategory>>() {});
            } catch (Exception e) {
                redisTemplate.delete(ALL_KEY);
            }
        }
        List<KnowledgeCategory> all = categoryMapper.selectList(null);
        try {
            redisTemplate.opsForValue().set(ALL_KEY, objectMapper.writeValueAsString(all), Duration.ofHours(1));
        } catch (Exception ignore) {
            // 缓存写失败不影响业务，下次读库兜底
        }
        return all;
    }

    private void evictAllCache() {
        redisTemplate.delete(ALL_KEY);
    }

    /** 校验父节点存在且层级关系合法：父节点层级必须 = 子层级 - 1 */
    private void validateParentAndLevel(Long parentId, Integer level) {
        if (parentId == null || parentId == 0L) {
            if (level != 1) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "顶层分类层级必须为 1（领域）");
            }
            return;
        }
        KnowledgeCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父分类不存在");
        }
        if (parent.getLevel() + 1 != level) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "父分类层级与子分类层级不匹配");
        }
        if (level > 3) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "分类层级最多 3 级");
        }
    }

    /** 构建树 */
    private List<CategoryNode> buildTree(List<KnowledgeCategory> all) {
        Map<Long, CategoryNode> nodeMap = new HashMap<>();
        for (KnowledgeCategory c : all) {
            CategoryNode node = new CategoryNode();
            node.setId(c.getId());
            node.setParentId(c.getParentId());
            node.setName(c.getName());
            node.setLevel(c.getLevel());
            node.setSort(c.getSort());
            nodeMap.put(c.getId(), node);
        }
        List<CategoryNode> roots = new ArrayList<>();
        for (CategoryNode node : nodeMap.values()) {
            if (node.getParentId() == null || node.getParentId() == 0L) {
                roots.add(node);
            } else {
                CategoryNode parent = nodeMap.get(node.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        return roots;
    }

    /** 收集某个节点下的全部子孙 ID */
    private List<Long> collectDescendantIds(Long id) {
        List<KnowledgeCategory> all = getAllCached();
        List<Long> result = new ArrayList<>();
        collectDescendants(all, id, result);
        return result;
    }

    private void collectDescendants(List<KnowledgeCategory> all, Long parentId, List<Long> result) {
        for (KnowledgeCategory c : all) {
            if (parentId.equals(c.getParentId())) {
                result.add(c.getId());
                collectDescendants(all, c.getId(), result);
            }
        }
    }

    /** 收集某节点下所有 level=3 的知识点 ID（目标节点自身若为 level=3 也纳入） */
    private List<Long> collectPointIds(List<KnowledgeCategory> all, Long categoryId) {
        Map<Long, List<KnowledgeCategory>> byParent = new HashMap<>();
        for (KnowledgeCategory c : all) {
            byParent.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c);
        }
        List<Long> points = new ArrayList<>();
        // 目标节点自身就是知识点时先纳入（AI 出题/兜底抽题常直接传 level=3 节点）
        for (KnowledgeCategory c : all) {
            if (categoryId.equals(c.getId()) && c.getLevel() != null && c.getLevel() == 3) {
                points.add(c.getId());
                break;
            }
        }
        collectPoints(byParent, categoryId, points);
        return points;
    }

    private void collectPoints(Map<Long, List<KnowledgeCategory>> byParent, Long id, List<Long> points) {
        for (KnowledgeCategory c : byParent.getOrDefault(id, List.of())) {
            if (c.getLevel() != null && c.getLevel() == 3) {
                points.add(c.getId());
            }
            collectPoints(byParent, c.getId(), points);
        }
    }
}
