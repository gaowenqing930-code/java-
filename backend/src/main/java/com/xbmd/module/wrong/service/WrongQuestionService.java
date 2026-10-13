package com.xbmd.module.wrong.service;

import com.xbmd.common.PageResult;
import com.xbmd.module.wrong.dto.WrongQuestionVO;

/**
 * 错题管理服务：自动收录、去重更新、筛选、移除
 */
public interface WrongQuestionService {

    /**
     * 答错自动收录：同一题目重复答错只更新次数与时间，不重复收录
     */
    void recordWrong(Long userId, Long questionId, Long answerRecordId);

    /**
     * 错题分页列表（按题型/关键词筛选，仅本人）
     */
    PageResult<WrongQuestionVO> pageQuery(Long userId, String type, String keyword,
                                          long pageNum, long pageSize);

    /**
     * 移除错题（标记已掌握/移除，仅本人）
     */
    void remove(Long id, Long userId);

    /**
     * 重练全部移除（错题重练练习全部答对后调用，可扩展）
     */
    int removeAll(Long userId);
}
