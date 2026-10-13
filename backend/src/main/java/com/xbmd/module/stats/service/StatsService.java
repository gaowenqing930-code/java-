package com.xbmd.module.stats.service;

import com.xbmd.module.stats.dto.StudyReportVO;

import java.util.List;

/**
 * 学习能力分析与报告服务
 */
public interface StatsService {

    /**
     * 答题后按题目挂载的知识点更新统计（答对/答错）
     */
    void updateStats(Long userId, Long questionId, boolean correct);

    /**
     * 学习报告：领域正确率、知识点掌握度、薄弱点 TopN、近 N 天趋势、训练建议
     */
    StudyReportVO report(Long userId, int days);

    /**
     * 薄弱知识点 ID 列表（样本≥3 且正确率<60%），供自适应策略优先推送
     */
    List<Long> listWeakPointIds(Long userId);
}
