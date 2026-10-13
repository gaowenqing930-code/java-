package com.xbmd.module.exam.service;

import com.xbmd.module.exam.dto.ExamPaperVO;

import java.util.List;

/**
 * 真题卷子服务：卷子列表 / 卷子详情（含固定题目）
 */
public interface ExamPaperService {

    /** 卷子列表（可按类型过滤：WRITTEN / INTERVIEW） */
    List<ExamPaperVO> listPapers(String paperType);

    /** 卷子详情（含按题号排序的固定题目） */
    ExamPaperVO getPaper(Long paperId);
}
