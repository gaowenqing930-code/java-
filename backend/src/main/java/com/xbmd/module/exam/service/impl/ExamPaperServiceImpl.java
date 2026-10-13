package com.xbmd.module.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xbmd.common.BusinessException;
import com.xbmd.common.ResultCode;
import com.xbmd.module.exam.dto.ExamPaperVO;
import com.xbmd.module.exam.entity.ExamPaper;
import com.xbmd.module.exam.entity.ExamPaperQuestion;
import com.xbmd.module.exam.mapper.ExamPaperMapper;
import com.xbmd.module.exam.mapper.ExamPaperQuestionMapper;
import com.xbmd.module.exam.service.ExamPaperService;
import com.xbmd.module.question.dto.QuestionVO;
import com.xbmd.module.question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 真题卷子服务实现：卷子 = 预置固定题目，打开直接整套作答
 */
@Service
@RequiredArgsConstructor
public class ExamPaperServiceImpl implements ExamPaperService {

    private final ExamPaperMapper examPaperMapper;
    private final ExamPaperQuestionMapper examPaperQuestionMapper;
    private final QuestionService questionService;

    @Override
    public List<ExamPaperVO> listPapers(String type) {
        LambdaQueryWrapper<ExamPaper> qw = new LambdaQueryWrapper<>();
        qw.eq(ExamPaper::getStatus, 1);
        if (StringUtils.hasText(type)) {
            qw.eq(ExamPaper::getType, type);
        }
        qw.orderByDesc(ExamPaper::getYear).orderByDesc(ExamPaper::getHeat).orderByDesc(ExamPaper::getId);
        return examPaperMapper.selectList(qw).stream().map(this::toVO).toList();
    }

    @Override
    public ExamPaperVO getPaper(Long paperId) {
        ExamPaper paper = examPaperMapper.selectById(paperId);
        if (paper == null || !Integer.valueOf(1).equals(paper.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "卷子不存在或已下架");
        }
        ExamPaperVO vo = toVO(paper);
        // 按题号顺序取固定题目
        List<ExamPaperQuestion> rels = examPaperQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamPaperQuestion>()
                        .eq(ExamPaperQuestion::getPaperId, paperId)
                        .orderByAsc(ExamPaperQuestion::getSort));
        List<QuestionVO> questions = new ArrayList<>();
        for (ExamPaperQuestion rel : rels) {
            QuestionVO detail = questionService.getDetail(rel.getQuestionId());
            if (detail != null) {
                questions.add(detail);
            }
        }
        vo.setQuestions(questions);
        return vo;
    }

    private ExamPaperVO toVO(ExamPaper paper) {
        ExamPaperVO vo = new ExamPaperVO();
        vo.setId(paper.getId());
        vo.setCompany(paper.getCompany());
        vo.setYear(paper.getYear());
        vo.setSeason(paper.getSeason());
        vo.setRound(paper.getRound());
        vo.setPosition(paper.getPosition());
        vo.setType(paper.getType());
        vo.setHeat(paper.getHeat());
        vo.setHot(paper.getHot());
        vo.setQuestionCount(paper.getQuestionCount());
        return vo;
    }
}
