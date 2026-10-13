package com.xbmd.module.wrong.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xbmd.common.BusinessException;
import com.xbmd.common.PageResult;
import com.xbmd.common.ResultCode;
import com.xbmd.module.practice.entity.WrongQuestion;
import com.xbmd.module.practice.mapper.WrongQuestionMapper;
import com.xbmd.module.question.entity.Question;
import com.xbmd.module.question.mapper.QuestionMapper;
import com.xbmd.module.question.service.QuestionService;
import com.xbmd.module.wrong.dto.WrongQuestionVO;
import com.xbmd.module.wrong.service.WrongQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 错题管理服务实现
 */
@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionService questionService;

    @Override
    @Transactional
    public void recordWrong(Long userId, Long questionId, Long answerRecordId) {
        WrongQuestion exist = wrongQuestionMapper.selectOne(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getQuestionId, questionId));
        if (exist == null) {
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setLastAnswerId(answerRecordId);
            wq.setWrongCount(1);
            wq.setStatus(1);
            wrongQuestionMapper.insert(wq);
        } else {
            if (exist.getStatus() != null && exist.getStatus() == 0) {
                // 曾移除又重新答错 → 重新收录
                exist.setStatus(1);
                exist.setWrongCount(1);
            } else {
                exist.setWrongCount((exist.getWrongCount() == null ? 0 : exist.getWrongCount()) + 1);
            }
            exist.setLastAnswerId(answerRecordId);
            wrongQuestionMapper.updateById(exist);
        }
    }

    @Override
    public PageResult<WrongQuestionVO> pageQuery(Long userId, String type, String keyword,
                                                 long pageNum, long pageSize) {
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getStatus, 1));
        if (wrongList.isEmpty()) {
            PageResult<WrongQuestionVO> empty = new PageResult<>();
            empty.setTotal(0);
            empty.setPageNum(pageNum);
            empty.setPageSize(pageSize);
            empty.setRecords(Collections.emptyList());
            return empty;
        }
        Map<Long, WrongQuestion> wrongMap = wrongList.stream()
                .collect(Collectors.toMap(WrongQuestion::getQuestionId, Function.identity()));
        List<Long> questionIds = wrongList.stream().map(WrongQuestion::getQuestionId).toList();

        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<Question>()
                .in(Question::getId, questionIds)
                .eq(Question::getStatus, 1);
        if (StringUtils.hasText(type)) {
            qw.eq(Question::getType, type.trim());
        }
        if (StringUtils.hasText(keyword)) {
            qw.like(Question::getStem, keyword.trim());
        }
        Page<Question> page = questionMapper.selectPage(new Page<>(pageNum, pageSize), qw);

        return PageResult.of(page, q -> {
            WrongQuestionVO vo = new WrongQuestionVO();
            WrongQuestion wq = wrongMap.get(q.getId());
            vo.setId(wq.getId());
            vo.setQuestion(questionService.getDetail(q.getId()));
            vo.setLastAnswerId(wq.getLastAnswerId());
            vo.setWrongCount(wq.getWrongCount());
            vo.setStatus(wq.getStatus());
            vo.setCreateTime(wq.getCreateTime());
            vo.setUpdateTime(wq.getUpdateTime());
            return vo;
        });
    }

    @Override
    public void remove(Long id, Long userId) {
        WrongQuestion wq = wrongQuestionMapper.selectById(id);
        if (wq == null || !wq.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "错题不存在");
        }
        wq.setStatus(0);
        wrongQuestionMapper.updateById(wq);
    }

    @Override
    @Transactional
    public int removeAll(Long userId) {
        List<WrongQuestion> wrongList = wrongQuestionMapper.selectList(
                new LambdaQueryWrapper<WrongQuestion>()
                        .eq(WrongQuestion::getUserId, userId)
                        .eq(WrongQuestion::getStatus, 1));
        for (WrongQuestion wq : wrongList) {
            wq.setStatus(0);
            wrongQuestionMapper.updateById(wq);
        }
        return wrongList.size();
    }
}
