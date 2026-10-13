package com.xbmd.module.question.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.PageResult;
import com.xbmd.common.ResultCode;
import com.xbmd.module.knowledge.entity.KnowledgeCategory;
import com.xbmd.module.knowledge.mapper.KnowledgeCategoryMapper;
import com.xbmd.module.knowledge.service.KnowledgeCategoryService;
import com.xbmd.module.question.dto.OptionDTO;
import com.xbmd.module.question.dto.QuestionQuery;
import com.xbmd.module.question.dto.QuestionRequest;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.dto.ScorePointDTO;
import com.xbmd.module.question.entity.Question;
import com.xbmd.module.question.entity.QuestionKnowledge;
import com.xbmd.module.question.enums.QuestionType;
import com.xbmd.module.question.mapper.QuestionKnowledgeMapper;
import com.xbmd.module.question.mapper.QuestionMapper;
import com.xbmd.module.question.service.QuestionService;
import com.xbmd.module.question.util.QuestionContentValidator;
import com.xbmd.module.question.util.QuestionFingerprintUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final String SOURCE_MANUAL = "MANUAL";
    private static final Integer STATUS_ON = 1;

    private final QuestionMapper questionMapper;
    private final QuestionKnowledgeMapper questionKnowledgeMapper;
    private final KnowledgeCategoryMapper categoryMapper;
    private final KnowledgeCategoryService categoryService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuestionVO create(QuestionRequest request) {
        // 内容校验（按题型）
        String normalizedAnswer = QuestionContentValidator.validateAndNormalize(
                request.getType(), request.getStem(), request.getOptions(), request.getAnswer(), request.getScorePoints());
        // 指纹判重
        String fingerprint = QuestionFingerprintUtil.compute(request.getStem(), request.getCategoryIds(), request.getDifficulty());
        if (fingerprintExists(fingerprint)) {
            throw new BusinessException(ResultCode.CONFLICT, "题库中已存在相同题目（指纹重复）");
        }

        Question question = new Question();
        question.setType(request.getType());
        question.setDifficulty(request.getDifficulty());
        question.setStem(request.getStem().trim());
        question.setOptionsJson(toJson(request.getOptions()));
        question.setAnswer(normalizedAnswer);
        question.setScorePointsJson(toJson(request.getScorePoints()));
        question.setAnalysis(request.getAnalysis());
        question.setSource(SOURCE_MANUAL);
        question.setFingerprint(fingerprint);
        question.setStatus(STATUS_ON);
        questionMapper.insert(question);

        saveLinks(question.getId(), request.getCategoryIds());
        return getDetail(question.getId());
    }

    @Override
    @Transactional
    public QuestionVO update(Long id, QuestionRequest request) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        String normalizedAnswer = QuestionContentValidator.validateAndNormalize(
                request.getType(), request.getStem(), request.getOptions(), request.getAnswer(), request.getScorePoints());

        String fingerprint = QuestionFingerprintUtil.compute(request.getStem(), request.getCategoryIds(), request.getDifficulty());
        Long dupCount = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getFingerprint, fingerprint).ne(Question::getId, id));
        if (dupCount != null && dupCount > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "题库中已存在相同题目（指纹重复）");
        }

        exist.setType(request.getType());
        exist.setDifficulty(request.getDifficulty());
        exist.setStem(request.getStem().trim());
        exist.setOptionsJson(toJson(request.getOptions()));
        exist.setAnswer(normalizedAnswer);
        exist.setScorePointsJson(toJson(request.getScorePoints()));
        exist.setAnalysis(request.getAnalysis());
        exist.setFingerprint(fingerprint);
        questionMapper.updateById(exist);

        // 重建关联
        questionKnowledgeMapper.delete(
                new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, id));
        saveLinks(id, request.getCategoryIds());
        return getDetail(id);
    }

    @Override
    public void delete(Long id) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        exist.setStatus(0);
        questionMapper.updateById(exist);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Question exist = questionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态仅支持 0/1");
        }
        exist.setStatus(status);
        questionMapper.updateById(exist);
    }

    @Override
    public PageResult<QuestionVO> pageQuery(QuestionQuery query) {
        // 解析分类过滤条件 → 知识点 ID 集合
        List<Long> pointIds = resolvePointIds(query);
        if (query.getPointId() != null || query.getDirectionId() != null || query.getDomainId() != null) {
            if (pointIds.isEmpty()) {
                return PageResult.of(new Page<>(query.getPageNum(), query.getPageSize()));
            }
        }

        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (!pointIds.isEmpty()) {
            String ids = pointIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.inSql(Question::getId, "SELECT question_id FROM question_knowledge WHERE category_id IN (" + ids + ")");
        }
        if (StringUtils.hasText(query.getType()) && QuestionType.isValid(query.getType())) {
            wrapper.eq(Question::getType, query.getType());
        }
        if (query.getDifficulty() != null) {
            wrapper.eq(Question::getDifficulty, query.getDifficulty());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(Question::getStem, query.getKeyword().trim());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Question::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Question::getCreateTime);

        Page<Question> page = questionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page, this::toVO);
    }

    @Override
    public QuestionVO getDetail(Long id) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "题目不存在");
        }
        return toVO(question);
    }

    @Override
    @Transactional
    public Question saveGenerated(Question question, List<Long> categoryIds) {
        String fingerprint = QuestionFingerprintUtil.compute(question.getStem(), categoryIds, question.getDifficulty());
        if (fingerprintExists(fingerprint)) {
            throw new BusinessException(ResultCode.CONFLICT, "题目指纹已存在");
        }
        question.setFingerprint(fingerprint);
        question.setStatus(STATUS_ON);
        questionMapper.insert(question);
        saveLinks(question.getId(), categoryIds);
        return question;
    }

    @Override
    public boolean fingerprintExists(String fingerprint) {
        if (!StringUtils.hasText(fingerprint)) {
            return false;
        }
        Long count = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>().eq(Question::getFingerprint, fingerprint));
        return count != null && count > 0;
    }

    @Override
    public Question getByFingerprint(String fingerprint) {
        return questionMapper.selectOne(
                new LambdaQueryWrapper<Question>().eq(Question::getFingerprint, fingerprint));
    }

    @Override
    public List<QuestionVO> randomPick(String type, Integer difficulty, List<Long> pointIds, int count) {
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(type) && QuestionType.isValid(type)) {
            wrapper.eq(Question::getType, type);
        }
        if (difficulty != null) {
            wrapper.eq(Question::getDifficulty, difficulty);
        }
        if (pointIds != null && !pointIds.isEmpty()) {
            String ids = pointIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            wrapper.inSql(Question::getId, "SELECT question_id FROM question_knowledge WHERE category_id IN (" + ids + ")");
        }
        wrapper.eq(Question::getStatus, 1);
        List<Question> candidates = questionMapper.selectList(wrapper);
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        Collections.shuffle(candidates);
        return candidates.stream().limit(Math.max(0, count)).map(this::toVO).toList();
    }

    /** 保存知识点关联 */
    private void saveLinks(Long questionId, List<Long> categoryIds) {
        // 知识点
        for (Long categoryId : categoryIds) {
            QuestionKnowledge link = new QuestionKnowledge();
            link.setQuestionId(questionId);
            link.setCategoryId(categoryId);
            questionKnowledgeMapper.insert(link);
        }
    }

    /** 把分类条件（领域/方向/知识点）解析为知识点 ID 集合 */
    private List<Long> resolvePointIds(QuestionQuery query) {
        if (query.getPointId() != null) {
            return List.of(query.getPointId());
        }
        if (query.getDirectionId() != null) {
            return categoryService.collectKnowledgePointIds(query.getDirectionId());
        }
        if (query.getDomainId() != null) {
            return categoryService.collectKnowledgePointIds(query.getDomainId());
        }
        return Collections.emptyList();
    }

    /** 实体 → VO（附带知识点名称与标签） */
    private QuestionVO toVO(Question question) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setType(question.getType());
        vo.setDifficulty(question.getDifficulty());
        vo.setStem(question.getStem());
        vo.setOptions(fromJson(question.getOptionsJson(), OptionDTO.class));
        vo.setAnswer(question.getAnswer());
        vo.setScorePoints(fromJson(question.getScorePointsJson(), ScorePointDTO.class));
        vo.setAnalysis(question.getAnalysis());
        vo.setSource(question.getSource());
        vo.setStatus(question.getStatus());
        vo.setCreateTime(question.getCreateTime());

        List<QuestionKnowledge> links = questionKnowledgeMapper.selectList(
                new LambdaQueryWrapper<QuestionKnowledge>().eq(QuestionKnowledge::getQuestionId, question.getId()));
        List<Long> categoryIds = links.stream().map(QuestionKnowledge::getCategoryId).toList();
        vo.setCategoryIds(categoryIds);
        if (!categoryIds.isEmpty()) {
            Map<Long, String> nameMap = categoryMapper.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getName));
            vo.setCategoryNames(categoryIds.stream().map(nameMap::get).filter(Objects::nonNull).toList());
        } else {
            vo.setCategoryNames(List.of());
        }
        return vo;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "JSON 序列化失败");
        }
    }

    private <T> List<T> fromJson(String json, Class<T> clazz) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }
}
