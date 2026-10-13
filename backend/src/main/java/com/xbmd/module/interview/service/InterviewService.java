package com.xbmd.module.interview.service;

import com.xbmd.module.interview.dto.InterviewAnswerRequest;
import com.xbmd.module.interview.dto.InterviewAnswerVO;
import com.xbmd.module.interview.dto.InterviewCreateRequest;
import com.xbmd.module.interview.dto.InterviewQuestionVO;
import com.xbmd.module.interview.dto.InterviewResultVO;
import com.xbmd.module.interview.dto.InterviewSessionVO;
import com.xbmd.module.interview.dto.ResumeParseVO;

import java.util.List;

/**
 * AI 智能面试服务
 */
public interface InterviewService {

    /** 解析简历并创建面试会话，返回技术栈与项目概览 */
    ResumeParseVO create(InterviewCreateRequest request, Long userId);

    /** 开始面试：按简历生成 10 道面试题，返回第一题 */
    InterviewQuestionVO start(Long sessionId, Long userId);

    /** 提交当前题作答，保存答案后 AI 引导式生成下一题；达到兜底上限时自动评分结束 */
    InterviewAnswerVO answer(Long sessionId, InterviewAnswerRequest request, Long userId);

    /** 出题失败后重试生成下一题（幂等：下一题已存在则直接返回） */
    InterviewQuestionVO nextQuestion(Long sessionId, Long userId);

    /** 用户主动结束面试：统一评分已答题目并生成总结（已结束会话幂等返回结果） */
    InterviewResultVO finish(Long sessionId, Long userId);

    /** 获取面试结果 */
    InterviewResultVO result(Long sessionId, Long userId);

    /** 当前用户的历史面试列表（按创建时间倒序） */
    List<InterviewSessionVO> mySessions(Long userId);
}
